package com.wjc.codetest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/*
 * 문제 : Global이라는 이름과 달리 특정 컨틀롤러의 예외만을 다루고 있습니다.
 * 원인 :
 * 개선안 :
 *         ControllerAdvice에 존재하는 특정 컨트롤러 지정(value)을 제거할 것을 권장드립니다.
 *
 *
 * 문제 : @ResponseBody가 최상단이 아닌 handler 하려는 예외마다 존재합니다.
 * 원인 : handle하려는 에러가 추가될 떄마다, @ResponseBody를 중복적으로 작성해야 하는 구조로 설계 되어있어, 추후에 코드의 가독성을 떨어트릴 수 있습니다.
 * 개선안 :
 *         최상단에 @ResponseBody로 설정 혹은 @RestControllerAdvice 정의를 권장드립니다.
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
 * 문제 : @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)와 ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)가 동시에 사용되고 있습니다.
 * 원인 : 동일한 동작을 하는 로직이 중복으로 선언되어있어, 코드의 가독성을 떨어트릴 수 있습니다.
 * 개선안 :
 *          @ResponseStatus 제거 혹은 ResponseEntity.status() 제거를 권장드립니다.
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
