package com.wjc.codetest.product.model.request;

import lombok.Getter;
import lombok.Setter;

/*
 * CreateProductRequest에 작성한 코드 리뷰와 동일합니다.
 */
@Getter
@Setter
public class UpdateProductRequest {
    private Long id;
    private String category;
    private String name;

    public UpdateProductRequest(Long id) {
        this.id = id;
    }

    public UpdateProductRequest(Long id, String category) {
        this.id = id;
        this.category = category;
    }

    public UpdateProductRequest(Long id, String category, String name) {
        this.id = id;
        this.category = category;
        this.name = name;
    }
}

