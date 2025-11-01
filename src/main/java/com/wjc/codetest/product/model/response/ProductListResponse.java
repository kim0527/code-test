package com.wjc.codetest.product.model.response;

import com.wjc.codetest.product.model.domain.Product;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * <p>
 *
 * </p>
 *
 * @author : 변영우 byw1666@wjcompass.com
 * @since : 2025-10-27
 */

/*
 * CreateProductRequest에 작성한 코드 리뷰와 동일합니다.
 */

/*
 * 문제 : Product(엔티티)엔티티를 반환하고 있습니다. (List<Product> products)
 * 원인 :
 *       엔티티를 반환하면 많은 문제점이 존재합니다.
 *
 *       1. 지연 로딩 문제 및 순환 참조 문제가 발생할 수 있습니다.
 *       현재는 엔티티의 연관관계 존재하지 않지만, 추후에 여러 엔티티가 추가되고 연관관계가 형성되었을때 문제가 발생합니다.
 *       [지연 로딩]
 *       많은 엔티티 설계시, N+1 문제를 방지하기 위해 FetchType.LAZY 설정을 많이 하게 됩니다.
 *       Product 객체 안에 있는 지연 로딩으로 설정되어 있는 A 엔티티가 존재한 상태에서,
 *       만약 영속성 컨텍스트 안에 A 엔티티 로드되지 않은 상태로 Product가 컨트롤러에서 반환되면, Jackson이 직렬화 하는 과정에서 LazyInitializationException가 발생합니다.
 *       [순환 참조]
 *       Product 객체 안에 양방향 관계로 맺여있는 A 엔티티가 존재하는 경우, Product가 컨트롤러에서 반환되면 Jackson이 직렬화 하는 과정에서 양방향 설정되어있는 관계로 인해 무한으로 순회하게 됩니다.
 *
 *       그 외에도 엔티티 정보가 외부에 그대로 노출되는 문제(보안 위험), API 응답이 DB 설계에 종속되는 문제 등이 발생할 수 있습니다.
 * 개선안 :
 *         1. Product를 대체할 별도의 dto 객체를 정의하는 것을 권장드립니다.
 *            불필요한 정보 노출을 방지할 수 있는 동시에, 아키텍처적인 계층 분리 또한 명확하게 할 수 있는 이점을 얻을 수 있습니다.
 */

@Getter
@Setter
public class ProductListResponse {
    private List<Product> products;
    private int totalPages;
    private long totalElements;
    private int page;

    public ProductListResponse(List<Product> content, int totalPages, long totalElements, int number) {
        this.products = content;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.page = number;
    }
}
