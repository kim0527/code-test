package com.wjc.codetest.product.model.request;

import lombok.Getter;
import lombok.Setter;

/*
 * CreateProductRequest에 작성한 코드 리뷰와 동일합니다.
 */
@Getter
@Setter
public class GetProductListRequest {
    private String category;
    private int page;
    private int size;
}