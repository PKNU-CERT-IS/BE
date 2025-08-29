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
 */
@Slf4j
@Component
public class DataConverter {

    /**
     * String 배열을 Long 리스트로 변환 (기본 메서드)
     */
    public List<Long> convertToLongList(String[] values) {
        return convertStringArray(values, Long::parseLong, "Long");
    }

    public List<Long> convertToLongList(Integer[] values) {
        return convertStringArray(values, Long::valueOf, "Long");
    }

    /**
     * String 배열을 Integer 리스트로 변환
     */
    public List<Integer> convertToIntegerList(String[] values) {
        return convertStringArray(values, Integer::parseInt, "Integer");
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

    /**
     * String 배열 전용 변환 메서드 (unchecked cast 없음)
     *
     * @param values 변환할 String 배열
     * @param converter String → R 변환 함수
     * @param typeName 타입명 (로깅용)
     * @param <R> 변환 결과 타입
     * @return 변환된 리스트
     */
    public <R> List<R> convertStringArray(String[] values, Function<String, R> converter, String typeName) {
        if (values == null || values.length == 0) {
            return Collections.emptyList();
        }

        return Arrays.stream(values)
                .filter(Objects::nonNull)
                .map(String::trim) // String이므로 안전하게 trim 가능
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

    public <R> List<R> convertStringArray(Integer[] values, Function<Integer, R> converter, String typeName) {
        if (values == null || values.length == 0) {
            return Collections.emptyList();
        }

        return Arrays.stream(values)
                .filter(Objects::nonNull) // null 체크만 수행
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
     *
     * @param values 변환할 String 컬렉션
     * @param converter String → R 변환 함수
     * @param typeName 타입명 (로깅용)
     * @param <R> 변환 결과 타입
     * @return 변환된 리스트
     */
    public <R> List<R> convertStringCollection(Collection<String> values, Function<String, R> converter, String typeName) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }

        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim) // String이므로 안전하게 trim 가능
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
     *
     * @param values 변환할 배열
     * @param converter 변환 함수
     * @param typeName 타입명 (로깅용)
     * @param <T> 변환 대상 타입
     * @param <R> 변환 결과 타입
     * @return 변환된 리스트
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
     *
     * @param values 변환할 컬렉션
     * @param converter 변환 함수
     * @param typeName 타입명 (로깅용)
     * @param <T> 변환 대상 타입
     * @param <R> 변환 결과 타입
     * @return 변환된 리스트
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
}