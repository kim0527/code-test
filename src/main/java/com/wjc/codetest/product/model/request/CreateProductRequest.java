package com.wjc.codetest.product.model.request;

import lombok.Getter;
import lombok.Setter;
/*
 * 문제 : @RequestBody에서 Json에서 객체로 역직렬화하는 과정 중 에러 발생
 * 원인 : 기본 생성자 부재로, Jackson이 인스턴스를 생성하지 못해 발생하는 에러
 * 개선안 :
 *         해결 방안    : Record 사용하기
 *                      1. Record는 데이터를 간결하게 표현하기 위해 등장한 것으로, 생성자, Getter와 같은 보일러플레이트 코드 반복 작성을 최소화 해줍니다.
 *                      2. 암묵적으로 필드에 final을 설정하여, 불변을 보장해줍니다.
 *
 *                      public record CreateProductRequest(
 *                          String category,
 *                          String name
 *                      ){ ... }
 *
 *                      위와 같이, Record 사용시 생성자를 자동 생성해주기 떄문에 코드가 더욱 간결해지며, 생성자 부재 런타임 에러도 방지 할 수 있다.
 *
 *         트레이드 오프 : Record 필드 유효성 검사 로직의 경우, Record 생성자 위치에 귀속되기 때문에, Record 생성자가 불필요하게 커질 수 있다.
 *
 *
 * 문제 : 매개변수에 따른 여러 생성자 정의
 * 원인 : 매개변수 상황에 따른 객체를 생성하기 위해 여러개의 생성자를 정의하여 코드의 가독성 감소
 * 개선안 :
 *         해결 방안 : 빌더 패턴 사용하기 (@Builder)
 *                   1. 매개 변수 상황에 따른 여러개의 생성자를 정의할 필요가 없어집니다. (코드의 가독성 증가)
 *                   2. 생성 시점에서, 파라미터가 여러 개인 new 생성자가 아니라 builder 패턴으로 명확한 인자 사용을 확인할 수 있다. (코드의 가독성 증가)
 *                      AS IS : new CreateProductRequest(category,name) --- (x)
 *                      TO BE : CreateProductRequest.builder()
 *                                             .category(category)
 *                                             .name(name)
 *                                             .build();
 */

@Getter
@Setter
public class CreateProductRequest {
    private String category;
    private String name;

    public CreateProductRequest(String category) {
        this.category = category;
    }

    public CreateProductRequest(String category, String name) {
        this.category = category;
        this.name = name;
    }
}

