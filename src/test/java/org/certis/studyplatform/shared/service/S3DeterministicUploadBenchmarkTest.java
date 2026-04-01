package org.certis.studyplatform.shared.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import static org.assertj.core.api.Assertions.assertThat;

class S3DeterministicUploadBenchmarkTest {

    private static final int TOTAL_REQUESTS = 1_000;
    private static final long HEAD_LATENCY_MILLIS = 2L;
    private static final long PUT_LATENCY_MILLIS = 25L;

    @Test
    void benchmarkDeterministicUploadWith1000Requests() throws Exception {
        long[] arrivalPlanMillis = createArrivalPlanMillis();
        byte[] payload = "hello-world".getBytes(StandardCharsets.UTF_8);

        StorageBenchmarkResult before = runLegacyDeterministicUpload(arrivalPlanMillis, payload);
        StorageBenchmarkResult after = runOptimizedDeterministicUpload(arrivalPlanMillis, payload);

        printSummary(before, after);

        assertThat(before.headOps()).isEqualTo(TOTAL_REQUESTS);
        assertThat(before.putOps()).isGreaterThan(1L);

        assertThat(after.headOps()).isEqualTo(1L);
        assertThat(after.putOps()).isEqualTo(1L);
        assertThat(after.totalS3OpsPerRequest()).isLessThan(before.totalS3OpsPerRequest());
    }

    private StorageBenchmarkResult runLegacyDeterministicUpload(long[] arrivalPlanMillis, byte[] payload) throws Exception {
        SimulatedS3Store store = new SimulatedS3Store();
        LegacyDeterministicUploader uploader = new LegacyDeterministicUploader(store);
        String objectKey = "idempotent-v1/1/bench-key/abcd1234.txt";

        long elapsedMillis = executeRequests(arrivalPlanMillis, requestIndex -> {
            sleepQuietly(arrivalPlanMillis[requestIndex]);
            uploader.upload(payload, objectKey);
        });

        return StorageBenchmarkResult.from("before", store, elapsedMillis);
    }

    private StorageBenchmarkResult runOptimizedDeterministicUpload(long[] arrivalPlanMillis, byte[] payload) throws Exception {
        SimulatedS3Store store = new SimulatedS3Store();
        S3AttachmentService service = mockedService(store);

        long elapsedMillis = executeRequests(arrivalPlanMillis, requestIndex -> {
            sleepQuietly(arrivalPlanMillis[requestIndex]);
            service.uploadBytesDeterministic(
                    payload,
                    "text/plain",
                    "memo.txt",
                    "study-attachments",
                    1L,
                    "bench-key"
            );
        });

        return StorageBenchmarkResult.from("after", store, elapsedMillis);
    }

    private long executeRequests(long[] arrivalPlanMillis, RequestWork requestWork) throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        long startedAt = System.nanoTime();
        try (ExecutorService executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>(TOTAL_REQUESTS);
            for (int requestIndex = 0; requestIndex < TOTAL_REQUESTS; requestIndex++) {
                final int currentRequestIndex = requestIndex;
                futures.add(executor.submit(() -> {
                    try {
                        startGate.await();
                        requestWork.run(currentRequestIndex);
                    } catch (Throwable throwable) {
                        errors.add(throwable);
                    }
                }));
            }

            startGate.countDown();
            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        }

        if (!errors.isEmpty()) {
            Throwable throwable = errors.peek();
            if (throwable instanceof Exception exception) {
                throw exception;
            }
            throw new RuntimeException(throwable);
        }

        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private long[] createArrivalPlanMillis() {
        long[] arrivalPlanMillis = new long[TOTAL_REQUESTS];
        Random random = new Random(42L);

        arrivalPlanMillis[0] = 0L;
        for (int i = 1; i < 100; i++) {
            arrivalPlanMillis[i] = random.nextInt(3);
        }
        for (int i = 100; i < 400; i++) {
            arrivalPlanMillis[i] = 4L + random.nextInt(17);
        }
        for (int i = 400; i < TOTAL_REQUESTS; i++) {
            arrivalPlanMillis[i] = 40L + random.nextInt(211);
        }
        return arrivalPlanMillis;
    }

    private void printSummary(StorageBenchmarkResult before, StorageBenchmarkResult after) {
        System.out.printf("%n[s3-deterministic-benchmark] totalRequests=%d, headLatencyMs=%d, putLatencyMs=%d%n",
                TOTAL_REQUESTS,
                HEAD_LATENCY_MILLIS,
                PUT_LATENCY_MILLIS
        );
        System.out.printf("%-10s | %12s | %12s | %18s | %10s%n",
                "scenario",
                "headOps",
                "putOps",
                "s3Ops/request",
                "elapsedMs");
        printRow(before);
        printRow(after);
        System.out.println();
    }

    private void printRow(StorageBenchmarkResult result) {
        System.out.printf("%-10s | %12d | %12d | %18.3f | %10d%n",
                result.scenario(),
                result.headOps(),
                result.putOps(),
                result.totalS3OpsPerRequest(),
                result.elapsedMillis());
    }

    private void sleepQuietly(long delayMillis) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static S3AttachmentService mockedService(SimulatedS3Store store) throws Exception {
        S3AttachmentService service = new S3AttachmentService();
        setField(service, "bucketName", "test-bucket");
        setField(service, "region", "ap-northeast-2");

        S3Client mockS3Client = Mockito.mock(S3Client.class);
        Mockito.when(mockS3Client.headObject(Mockito.any(HeadObjectRequest.class)))
                .thenAnswer(invocation -> {
                    HeadObjectRequest request = invocation.getArgument(0, HeadObjectRequest.class);
                    if (store.headObject(request.key())) {
                        return HeadObjectResponse.builder()
                                .contentLength((long) payloadLength())
                                .contentType("text/plain")
                                .build();
                    }
                    throw NoSuchKeyException.builder().message("missing").build();
                });
        Mockito.when(mockS3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenAnswer(invocation -> {
                    PutObjectRequest request = invocation.getArgument(0, PutObjectRequest.class);
                    store.putObject(request.key());
                    return PutObjectResponse.builder().eTag("etag").build();
                });

        setField(service, "s3Client", mockS3Client);
        getMapField(service, "s3ClientByRegion").put("ap-northeast-2", mockS3Client);
        return service;
    }

    private static int payloadLength() {
        return "hello-world".getBytes(StandardCharsets.UTF_8).length;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, S3Client> getMapField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (Map<String, S3Client>) field.get(target);
    }

    private record StorageBenchmarkResult(
            String scenario,
            long headOps,
            long putOps,
            long elapsedMillis
    ) {
        private static StorageBenchmarkResult from(String scenario, SimulatedS3Store store, long elapsedMillis) {
            return new StorageBenchmarkResult(
                    scenario,
                    store.headOps(),
                    store.putOps(),
                    elapsedMillis
            );
        }

        private double totalS3OpsPerRequest() {
            return (headOps + putOps) / (double) TOTAL_REQUESTS;
        }
    }

    private static class LegacyDeterministicUploader {

        private final SimulatedS3Store store;

        private LegacyDeterministicUploader(SimulatedS3Store store) {
            this.store = store;
        }

        private String upload(byte[] bytes, String objectKey) {
            if (bytes == null || bytes.length == 0) {
                throw new IllegalArgumentException("bytes must not be empty");
            }

            if (store.headObject(objectKey)) {
                return objectKey;
            }

            store.putObject(objectKey);
            return objectKey;
        }
    }

    private static class SimulatedS3Store {

        private final Set<String> objectKeys = ConcurrentHashMap.newKeySet();
        private final LongAdder headOps = new LongAdder();
        private final LongAdder putOps = new LongAdder();

        private boolean headObject(String key) {
            headOps.increment();
            sleep(HEAD_LATENCY_MILLIS);
            return objectKeys.contains(key);
        }

        private void putObject(String key) {
            putOps.increment();
            sleep(PUT_LATENCY_MILLIS);
            objectKeys.add(key);
        }

        private long headOps() {
            return headOps.sum();
        }

        private long putOps() {
            return putOps.sum();
        }

        private void sleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    @FunctionalInterface
    private interface RequestWork {
        void run(int requestIndex);
    }
}
