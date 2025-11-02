package com.wjc.codetest.product.model.request;

import lombok.Getter;
import lombok.Setter;
/*
 * 문제 : @RequestBody에서 Json에서 객체로 역직렬화하는 과정 중 JSON parse error가 발생합니다.
 * 원인 : 기본 생성자가 존재하지 않아, Json에서 requestDto로 역직렬화하는 과정에서 Jackson이 인스턴스를 생성하지 못하여 발생하였습니다.
 * 개선안 :
 *         1. Record를 활용할 것을 권장드립니다.
 *            Record는 데이터를 간결하게 표현하기 위해 등장한 것으로, 생성자, Getter와 같은 보일러플레이트 코드 반복 작성을 최소화해 줍니다.
 *            그리고 암묵적으로 필드에 final을 설정하여, 불변을 보장해줍니다.
 *
 *            Record 사용 시 생성자를 자동 생성해주기 때문에 간결한 코드 이점을 가질 수 있으며, JSON parse error와 같은 런타임 에러도 방지할 수 있습니다.
 *            [예시 코드]
 *            public record CreateProductRequest(
 *                String category,
 *                String name
 *            ){ ... }
 *
 * 트레이드 오프 : Record 필드 유효성 검사 로직의 경우, Record 생성자 위치에 귀속되기 때문에, Record 생성자가 불필요하게 커질 수 있습니다.
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

