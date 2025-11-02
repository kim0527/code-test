package com.wjc.codetest.product.controller;

import com.wjc.codetest.product.model.request.CreateProductRequest;
import com.wjc.codetest.product.model.request.GetProductListRequest;
import com.wjc.codetest.product.model.domain.Product;
import com.wjc.codetest.product.model.request.UpdateProductRequest;
import com.wjc.codetest.product.model.response.ProductListResponse;
import com.wjc.codetest.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * 문제 : 요청 url에 Request의 리소스, 행위 정보가 모두 포함되어 있습니다.
 *       예시) /get/product/by/{productId}, /create/product, /delete/product/{productId}
 * 원인 :
 *       REST는 HTTP 프로토콜을 활용하여 통신할때 적용되는 통신 규약(규칙)으로, 리소스 중심의 API 설계하는 것이 특징입니다.
 *       리소스는 url에, 행위는 HTTP 메서드로 표현하여 리소스와 행위를 분리하는 규칙을 가지고 있습니다.
 *
 *       Hateoas 등 REST 설계 원칙으로 인해 100% RESTful한 URL 설계에 도달하기에는 어려움이 있지만,
 *       해당 규칙을 반영한다면 HTTP 프로토콜을 더욱 활용하는 동시에, 일관되고 간결한 url을 제공할 수 있습니다.
 *
 *       rer) https://docs.microsoft.com/en-us/azure/architecture/best-practices/api-design
 * 개선안 :
 *          1. API의 행위의 경우, HTTP 메서드로 표현하는 것을 권장합니다.
 *             [예시]
 *             CREATE -> POST Mapping
 *             READ   -> GET Mapping
 *             UPDATE -> PUT / PATCH Mapping
 *             DELETE -> DELETE Mapping
 *
 *          2. API의 리소스(자원)의 경우, url 상에서 복수 명사로 표시하는 것을 권장합니다.
 *             [예시]
 *             product 전체조회 -> @GetMapping(value = "/products")
 *             product 상세조회 -> @GetMapping(value = "/products/{productId}")
 *             product 업데이트 -> @PostMapping(value = "/products/{productId}")
 *             product 삭제    -> @DeleteMapping(value = "/products/{productId}")
 *
 *          3. 여러 url에서 공통되는 부분의 경우, @RequestMapping을 활용해, 중복된 코드 작성 최소화
 *             [예시]
 *             @RequestMapping("/products")
 *
 *
 * 문제 : 컨트롤러에서 Boolean, List<String>를 반환하고 있습니다.
 *       ( 대상 메서드 : deleteProduct(), getProductListByCategory() )
 * 원인 :
 *       코드 작성의 편리함은 있지만 박스 타입, 컬렉션 타입 반환시, 2가지 단점이 있습니다.
 *
 *       1. 응답의 가독성이 떨어지며, 명확한 의미 전달이 어려워집니다.
 *       Boolean의 경우 true, false를, 컬렉션 타입의 경우, 컬렉션의 원소 정보를 반환하는데,
 *       해당 value가 의마하는 정보 없이 value만 반환하기 때문에, FE에서 응답 본문만으로 파악하기에는 어려움이 발생합니다.
 *       FE에 데이터를 명확하게 전달하는 것이 BE의 역할이라 생각합니다.
 *
 *       2. API의 확장성이 떨어지게 됩니다.
 *       추후에 요청 시간, 메세지와 같은 메타데이터 추가되어야 하는 상황이라면, API 스펙 자체가 바뀌는 문제가 있습니다.
 *
 *       실제로 많은 레퍼런스에서 원시타입, 박스타입, 컬렉션 타입 자체를 반환하는 것을 지양하고 있습니다.
 *
 * 개선안 :
 *         1. 별도의 Response dto 정의하는 것을 권장드립니다.
 *            [예시 코드]
 *            public record DeleteProductResponse(
 *               Boolean result,
 *               String message
 *            ){ ... }
 *
 *         2. (Boolean 타입의 경우) HTTP status 활용을 권장드립니다.
 *            deleteProduct()에서는 true는 삭제 성공 의미로 해석됩니다.
 *            HTTP status는 HTTP 요청이 어떻게 처리되었는지를 나타내는 역할을 합니다.
 *            성공의 경우 200을, 실패의 경우 400로 반환하는 것이 HTTP 프로토콜을 올바르게 활용하는 것이라 생각합니다.
 *            [예시 코드]
 *            성공 -> return ResponseEntity.status(HttpStatus.OK).body(response);
 *            실패 ->return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
 *
 *
 * 문제 : 컨트롤러에서 Product(엔티티)엔티티를 반환하고 있습니다.
 *       ( 대상 메서드 : getProductById(), createProduct(), updateProduct() )
 * 원인 :
 *       컨트롤러에서 엔티티를 반환하면 많은 문제점이 존재합니다.
 *
 *       1. 지연 로딩 문제 및 순환 참조 문제가 발생할 수 있습니다.
 *       현재는 엔티티의 연관관계 존재하지 않지만, 추후에 여러 엔티티가 추가되고 연관관계가 형성되었을때 문제가 발생합니다.
 *       [지연 로딩]
 *       많은 엔티티 설계시, N+1 문제를 방지하기 위해 FetchType.LAZY 설정을 많이 하게 됩니다.
 *       Product 객체 안에 있는 지연 로딩으로 설정되어 있는 A 엔티티가 존재한 상태에서,
 *       만약 영속성 컨텍스트 안에, A 엔티티가 로드되지 않은 상태로, Product가 컨트롤러에서 반환되면 Jackson이 직렬화 하는 과정에서 LazyInitializationException가 발생합니다.
 *       [순환 참조]
 *       Product 객체 안에 양방향 관계로 맺여있는 A 엔티티가 존재하는 경우, Product가 컨트롤러에서 반환되면 Jackson이 직렬화 하는 과정에서 양방향 설정되어있는 관계로 인해 무한으로 순회하게 됩니다.
 *
 *       2. 설계 관점에서 컨트롤러의 올바른 역할과 멀어집니다.
 *       컨트롤러는 클라이언트에서 필요한 정보를 반환하는 역할을 합니다.
 *       엔티티는 서비스의 핵심 도메인 객체로써, "클라이언트에서 필요한 정보"와는 역할이 다르며, 생명주기 또한 다르다고 생각합니다.
 *       그래서 클라이언트의 필요한 정보, response dto로써 전달하는 것이 더 올바르다 생각합니다.
 *
 *       그 외에도 엔티티 정보가 외부에 그대로 노출되는 문제(보안 위험), API 응답이 DB 설계에 종속되는 문제 등이 발생할 수 있습니다.
 * 개선안 :
 *         1. 클라이이언트 전용 응답 객체인, Response dto를 정의하여 반환하는 것을 권장드립니다.
 *            dto는 data transfer object의 약자로, 데이터 전송 객체를 의미합니다.
 *            필요한 핵심 정보를 반환하여, 불필요한 정보 노출을 방지할 수 있는 동시에, 아키텍처적인 계층 분리 또한 명확하게 할 수 있는 이점을 얻을 수 있습니다.
 *            [예시 코드]
 *            public record DeleteProductResponse(
 *               Boolean result,
 *               String message
 *            ){ ... }
 *
 *         + 공통 응답 정의하기
 *         공통 응답 정의시, 시스템의 API Response 스펙을 공통화 할 수 있으며, 프론트엔드 단에서 백엔드 응답 데이터를 일관된 방식으로 처리할 수 있는 이점을 얻을 수 있습니다.
 *         [예시 코드]
 *          public record ApiResponse<T>(
 *              String message,
 *              T data,
 *          ) {}
 *
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping(value = "/get/product/by/{productId}")
    public ResponseEntity<Product> getProductById(@PathVariable(name = "productId") Long productId){
        Product product = productService.getProductById(productId);
        return ResponseEntity.ok(product);
    }

    @PostMapping(value = "/create/product")
    public ResponseEntity<Product> createProduct(@RequestBody CreateProductRequest dto){
        Product product = productService.create(dto);
        return ResponseEntity.ok(product);
    }

    @PostMapping(value = "/delete/product/{productId}")
    public ResponseEntity<Boolean> deleteProduct(@PathVariable(name = "productId") Long productId){
        productService.deleteById(productId);
        return ResponseEntity.ok(true);
    }

    @PostMapping(value = "/update/product")
    public ResponseEntity<Product> updateProduct(@RequestBody UpdateProductRequest dto){
        Product product = productService.update(dto);
        return ResponseEntity.ok(product);
    }

    /*
     * 문제 : 조회 API임에도 불구하고, HTTP 메서드 POST를 설정했습니다.
     * 원인 :
     *          1. 해당 API 역할과 HTTP 메서드가 매칭되지 않습니다.
     *             POST 메서드 사용는 리소스를 새로 생성하거나 상태를 변경하는 의미를 내포하고 있습니다.
     *             반면 GET 메서드는 리소스 읽기 작업의 의미를 내포하고 있고, 멱등성을 보장하고 있어, 해당 API 역할에 의미론적으로 적합합니다.
     *
     * 개선안 :
     *          1. GET 메서드로 변경하고, 검색 조건은 Request Parameter로 전달하는 것을 권장합니다.
     *          조건 데이터를 RequestBody로 받기 위해 POST를 활용한 것으로 추측됩니다.
     *          하지만 해당 API는 특정 조건으로 조회하는 로직이기 때문에 GET 메서드가 올바릅니다.
     *          물론 과거와 달리, GET 메서드 사용시, Body를 활용하는 것이 허용되었지만, 좋은 설계와는 거리가 멀다고 생각합니다.
     *
     *          추가로 GET 메서드 사용시, 브러우자로부터 응답값 캐시 이점을 얻을 수 있습니다.
     *          -------------------------------------------------------------------------------------------
     *          HTTP 공식 문서
     *          The GET method means retrieve whatever information ([...]) is identified by the Request-UR
     *          -------------------------------------------------------------------------------------------
     *
     *          public ResponseEntity<ProductListResponse> getProductListByCategory(
     *                  @RequestParam String category,
     *                  @RequestParam(defaultValue = "0") int page,
     *                  @RequestParam(defaultValue = "10") int size
     *          ){ ... }
     */
    @PostMapping(value = "/product/list")
    public ResponseEntity<ProductListResponse> getProductListByCategory(@RequestBody GetProductListRequest dto){
        Page<Product> productList = productService.getListByCategory(dto);
        return ResponseEntity.ok(new ProductListResponse(productList.getContent(), productList.getTotalPages(), productList.getTotalElements(), productList.getNumber()));
    }

    @GetMapping(value = "/product/category/list")
    public ResponseEntity<List<String>> getProductListByCategory(){
        List<String> uniqueCategories = productService.getUniqueCategories();
        return ResponseEntity.ok(uniqueCategories);
    }
}