package com.wjc.codetest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/*
 * 문제 : Global이라는 이름과는 거리가 먼 Handler 정의
 * 원인 :
 * 개선안 :
 *         ControllerAdvice에 존재하는 특정 컨트롤러 지정(value) 제거
 *
 *
 * 문제 : Handle하려는 에러가 추가될 떄마다, @ResponseBody 반복 코드를 추가하게 됩니다.
 * 원인 : @ExceptionHandler가 추가될때마다, @ResponseBody를 추가해줘야 해기 때문에
 * 개선안 :
 *         최상단에 @RestControllerAdvice 정의하기
 *         [@RestControllerAdvice 내부 코드]
 *          @Target({ElementType.TYPE})
 *          @Retention(RetentionPolicy.RUNTIME)
 *          @Documented
 *          @ControllerAdvice
 *          @ResponseBody
 *          public @interface RestControllerAdvice { ... }
 *
 *         위와 같이 @RestControllerAdvice 내부에는 @ControllerAdvice와 @ResponseBody를 포함하고 있어, 기존과 동일하게 동작할 수 있습니다.
 *         최상단에 @RestControllerAdvice를 사용한다면 반복적인 @ResponseBody 사용을 방지할 수 있습니다.
 *
 *
 * 문제 : 중복된 Response Status 정의
 * 원인 :
 * 개선안 :
 *          @ResponseStatus 혹은 ResponseEntity.status()에서 Status 정의 둘 중 한가지만 하기
 *
 *
 * + 시스템 전역 에러 코드 정의 하기
 *   전역 에러 코드를 따로 정의한다면, 일관된 에러 응답을 제공할 수 있고, Enum으로 관리함으로써 오타와 같은 휴먼 에러를 줄일 수 있습니다.
 *   그리고 명확한 의미 전달도 가능해지며, 재사용성을 높일 수 있습니다.
 *
 *   [예시 코드]
 *   @Getter
 *   @JsonFormat(shape = JsonFormat.Shape.OBJECT)
 *   public enum ErrorCode{
 *
 *      INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST.value(), "C001", " 잘못된 입력값입니다."),
 *      METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED.value(), "C002", " 요청메서드가 허용되지 않습니다."),
 *      ...
 *
 *      private final int status;
 *      private final String code;
 *      private String message;
 *   }
 */

@Slf4j
@ControllerAdvice(value = {"com.wjc.codetest.product.controller"})
public class GlobalExceptionHandler {

    @ResponseBody
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<String> runTimeException(Exception e) {
        log.error("status :: {}, errorType :: {}, errorCause :: {}",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "runtimeException",
                e.getMessage()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
