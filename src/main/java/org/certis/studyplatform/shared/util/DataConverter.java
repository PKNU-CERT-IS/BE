package org.certis.studyplatform.shared.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 범용 데이터 변환 유틸리티
 *
 * 다양한 타입의 배열/컬렉션을 다른 타입의 리스트로 변환하는 기능 제공
 * ✅ unchecked cast 경고 해결
 * ✅ Long ↔ Integer 상호 변환 지원
 */
@Slf4j
@Component
public class DataConverter {

    // =================================================================
    // Long 관련 변환 메서드들
    // =================================================================

    /**
     * String 배열을 Long 리스트로 변환 (기본 메서드)
     */
    public List<Long> convertToLongList(String[] values) {
        return convertStringArray(values, Long::parseLong, "Long");
    }

    /**
     * Long 배열을 Long 리스트로 변환 (수정됨)
     */
    public List<Long> convertToLongList(Long[] values) {
        return convertArray(values, Function.identity(), "Long");
    }

    /**
     * Integer 배열을 Long 리스트로 변환
     */
    public List<Long> convertToLongList(Integer[] values) {
        return convertArray(values, Integer::longValue, "Long");
    }

    /**
     * CSV 문자열을 Long 리스트로 변환
     */
    public List<Long> convertCsvToLongList(String csvValues) {
        return convertCsvToLongList(csvValues, ",");
    }

    /**
     * 구분자를 지정한 CSV 문자열을 Long 리스트로 변환
     */
    public List<Long> convertCsvToLongList(String csvValues, String delimiter) {
        if (csvValues == null || csvValues.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String[] values = csvValues.split(delimiter);
        return convertToLongList(values);
    }

    /**
     * String 리스트를 Long 리스트로 변환
     */
    public List<Long> convertToLongList(List<String> values) {
        return convertStringCollection(values, Long::parseLong, "Long");
    }

    // =================================================================
    // Integer 관련 변환 메서드들
    // =================================================================

    /**
     * String 배열을 Integer 리스트로 변환
     */
    public List<Integer> convertToIntegerList(String[] values) {
        return convertStringArray(values, Integer::parseInt, "Integer");
    }

    /**
     * Long 배열을 Integer 리스트로 변환
     */
    public List<Integer> convertToIntegerList(Long[] values) {
        return convertArray(values, Long::intValue, "Integer");
    }

    /**
     * Integer 배열을 Integer 리스트로 변환
     */
    public List<Integer> convertToIntegerList(Integer[] values) {
        return convertArray(values, Function.identity(), "Integer");
    }

    /**
     * String 리스트를 Integer 리스트로 변환
     */
    public List<Integer> convertToIntegerList(List<String> values) {
        return convertStringCollection(values, Integer::parseInt, "Integer");
    }

    // =================================================================
    // Long ↔ Integer 배열 상호 변환 메서드들 (새로 추가)
    // =================================================================

    /**
     * Long 배열을 Integer 배열로 변환 (jOOQ 호환성을 위해)
     */
    public Integer[] convertLongArrayToIntegerArray(Long[] longArray) {
        if (longArray == null) {
            return null;
        }

        return Arrays.stream(longArray)
                .filter(Objects::nonNull)
                .map(value -> {
                    try {
                        return Math.toIntExact(value); // overflow 체크
                    } catch (ArithmeticException e) {
                        log.warn("Long value {} is too large for Integer, using MAX_VALUE", value);
                        return Integer.MAX_VALUE;
                    }
                })
                .toArray(Integer[]::new);
    }

    /**
     * Integer 배열을 Long 배열로 변환
     */
    public Long[] convertIntegerArrayToLongArray(Integer[] intArray) {
        if (intArray == null) {
            return null;
        }

        return Arrays.stream(intArray)
                .filter(Objects::nonNull)
                .map(Integer::longValue)
                .toArray(Long[]::new);
    }

    /**
     * Long 리스트를 Integer 배열로 변환
     */
    public Integer[] convertLongListToIntegerArray(List<Long> longList) {
        if (longList == null || longList.isEmpty()) {
            return new Integer[0];
        }

        return longList.stream()
                .filter(Objects::nonNull)
                .map(value -> {
                    try {
                        return Math.toIntExact(value);
                    } catch (ArithmeticException e) {
                        log.warn("Long value {} is too large for Integer, using MAX_VALUE", value);
                        return Integer.MAX_VALUE;
                    }
                })
                .toArray(Integer[]::new);
    }

    /**
     * Integer 리스트를 Long 배열로 변환
     */
    public Long[] convertIntegerListToLongArray(List<Integer> intList) {
        if (intList == null || intList.isEmpty()) {
            return new Long[0];
        }

        return intList.stream()
                .filter(Objects::nonNull)
                .map(Integer::longValue)
                .toArray(Long[]::new);
    }

    // =================================================================
    // 범용 변환 메서드들
    // =================================================================

    /**
     * String 배열 전용 변환 메서드 (unchecked cast 없음)
     */
    public <R> List<R> convertStringArray(String[] values, Function<String, R> converter, String typeName) {
        if (values == null || values.length == 0) {
            return Collections.emptyList();
        }

        return Arrays.stream(values)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> {
                    try {
                        return converter.apply(value);
                    } catch (Exception e) {
                        log.warn("Failed to convert value '{}' to {}: {}", value, typeName, e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * String 컬렉션 전용 변환 메서드 (unchecked cast 없음)
     */
    public <R> List<R> convertStringCollection(Collection<String> values, Function<String, R> converter, String typeName) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }

        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> {
                    try {
                        return converter.apply(value);
                    } catch (Exception e) {
                        log.warn("Failed to convert value '{}' to {}: {}", value, typeName, e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 범용 배열 변환 메서드 (String이 아닌 타입용)
     */
    public <T, R> List<R> convertArray(T[] values, Function<T, R> converter, String typeName) {
        if (values == null || values.length == 0) {
            return Collections.emptyList();
        }

        return Arrays.stream(values)
                .filter(Objects::nonNull)
                .map(value -> {
                    try {
                        return converter.apply(value);
                    } catch (Exception e) {
                        log.warn("Failed to convert value '{}' to {}: {}", value, typeName, e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 범용 컬렉션 변환 메서드 (String이 아닌 타입용)
     */
    public <T, R> List<R> convertCollection(Collection<T> values, Function<T, R> converter, String typeName) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }

        return values.stream()
                .filter(Objects::nonNull)
                .map(value -> {
                    try {
                        return converter.apply(value);
                    } catch (Exception e) {
                        log.warn("Failed to convert value '{}' to {}: {}", value, typeName, e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // =================================================================
    // 안전한 변환 메서드들 (기본값 포함)
    // =================================================================

    /**
     * 안전한 String 배열 변환 (예외 발생 시 기본값 반환)
     */
    public <R> List<R> convertStringArraySafe(String[] values, Function<String, R> converter, R defaultValue, String typeName) {
        if (values == null || values.length == 0) {
            return Collections.emptyList();
        }

        return Arrays.stream(values)
                .filter(Objects::nonNull)
                .map(value -> {
                    try {
                        String trimmedValue = value.trim();
                        if (trimmedValue.isEmpty()) {
                            return defaultValue;
                        }
                        return converter.apply(trimmedValue);
                    } catch (Exception e) {
                        log.warn("Failed to convert value '{}' to {}, using default: {}", value, typeName, defaultValue);
                        return defaultValue;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 범용 안전한 배열 변환 (예외 발생 시 기본값 반환)
     */
    public <T, R> List<R> convertArraySafe(T[] values, Function<T, R> converter, R defaultValue, String typeName) {
        if (values == null || values.length == 0) {
            return Collections.emptyList();
        }

        return Arrays.stream(values)
                .filter(Objects::nonNull)
                .map(value -> {
                    try {
                        return converter.apply(value);
                    } catch (Exception e) {
                        log.warn("Failed to convert value '{}' to {}, using default: {}", value, typeName, defaultValue);
                        return defaultValue;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 안전한 Long[]을 Integer[] 변환 (오버플로우 체크 포함)
     */
    public Integer[] convertLongArrayToIntegerArraySafe(Long[] longArray, Integer defaultValue) {
        if (longArray == null) {
            return null;
        }

        return Arrays.stream(longArray)
                .filter(Objects::nonNull)
                .map(value -> {
                    try {
                        return Math.toIntExact(value);
                    } catch (ArithmeticException e) {
                        log.warn("Long value {} is too large for Integer, using default: {}", value, defaultValue);
                        return defaultValue;
                    }
                })
                .toArray(Integer[]::new);
    }

    // =================================================================
    // 검증 및 중복 제거 메서드들
    // =================================================================

    /**
     * 정규식을 사용한 검증과 함께 변환
     */
    public List<Long> convertToLongListWithValidation(String[] values, String validationPattern) {
        if (values == null || values.length == 0) {
            return Collections.emptyList();
        }

        return Arrays.stream(values)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .filter(value -> validationPattern == null || value.matches(validationPattern))
                .map(value -> {
                    try {
                        return Long.parseLong(value);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid number format: {}", value);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 중복 제거와 함께 String 배열 변환
     */
    public <R> List<R> convertStringArrayDistinct(String[] values, Function<String, R> converter, String typeName) {
        return convertStringArray(values, converter, typeName)
                .stream()
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 중복 제거와 함께 범용 배열 변환
     */
    public <T, R> List<R> convertArrayDistinct(T[] values, Function<T, R> converter, String typeName) {
        return convertArray(values, converter, typeName)
                .stream()
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * String 배열을 Set으로 변환 (중복 자동 제거)
     */
    public <R> Set<R> convertStringArrayToSet(String[] values, Function<String, R> converter, String typeName) {
        return new HashSet<>(convertStringArray(values, converter, typeName));
    }

    /**
     * 범용 배열을 Set으로 변환 (중복 자동 제거)
     */
    public <T, R> Set<R> convertArrayToSet(T[] values, Function<T, R> converter, String typeName) {
        return new HashSet<>(convertArray(values, converter, typeName));
    }

    // =================================================================
    // 편의 메서드들 (자주 사용되는 변환)
    // =================================================================

    /**
     * 문자열 ID 목록을 Long 배열로 변환 (jOOQ용)
     */
    public Long[] convertStringIdsToLongArray(String csvIds) {
        List<Long> longList = convertCsvToLongList(csvIds);
        return longList.toArray(new Long[0]);
    }

    /**
     * 문자열 ID 목록을 Integer 배열로 변환 (jOOQ용)
     */
    public Integer[] convertStringIdsToIntegerArray(String csvIds) {
        List<Long> longList = convertCsvToLongList(csvIds);
        return convertLongListToIntegerArray(longList);
    }

    /**
     * Long ID 배열을 CSV 문자열로 변환
     */
    public String convertLongArrayToCsv(Long[] longArray) {
        if (longArray == null || longArray.length == 0) {
            return "";
        }

        return Arrays.stream(longArray)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    /**
     * Integer ID 배열을 CSV 문자열로 변환
     */
    public String convertIntegerArrayToCsv(Integer[] intArray) {
        if (intArray == null || intArray.length == 0) {
            return "";
        }

        return Arrays.stream(intArray)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}