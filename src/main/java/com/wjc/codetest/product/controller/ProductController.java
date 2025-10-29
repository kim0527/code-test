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
 * 문제 : 요청 url 명명 규칙이 RESTful 하지 않습니다.
 * 원인 : 요청에 대한 도메인, 요청의 행위를 url 모두 포함시켰습니다.
 * 개선안 :
 *          1. REST API에서 행위의 경우, HTTP 메서드로 구분합니다.
 *             url에 행위를 표현하는 동사는 쓰지 않습니다.
 *             [예시]
 *             CREATE -> POST Mapping
 *             READ   -> GET Mapping
 *             UPDATE -> POST / PATCH Mapping
 *             DELETE -> DELETE Mapping
 *
 *          2. REST API에서 자원의 경우, url 상에서 표시합니다.
 *             자원은 여러 개이기 때문에, 단수 명서가 아닌 복수 명사로 표현합니다.
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
 * 문제 : Controller에서의 원시타입 혹은 컬렉션 타입 반환으로 인해, API 응답값 확인 시, 어떠한 데이터인지 파악하기 어려움이 있습니다.
 *       ( 대상 메서드 : deleteProduct(), getProductListByCategory() )
 * 원인 :
 * 개선안 :
 *         1. 별도의 Response dto 정의하기
 *            [예시 코드]
 *            public record DeleteProductResponse(
 *               Boolean result,
 *               String message
 *            ){ ... }
 *
 *
 * 문제 : Controller에서의 엔티티 반환으로 인해, 엔티티 정보가 외부에 그대로 노출되게 되고(보안 위험), API 응답이 DB 설계에 종속됩니다.
 *       ( 대상 메서드 : getProductById(), createProduct(), updateProduct() )
 * 원인 :
 * 개선안 :
 *         1. Service 레이어에서 별도의 Response dto로 변환후 반환하기
 *
 *
 * + 공통 응답 정의하기
 *   공통 응답 정의시, 시스템의 API Response 스펙을 공통화 할 수 있고, FE에서 데이터를 일관된 방식으로 처리할 수 있게 됩니다.
 *   [예시 코드]
 *   public record ApiResponse<T>(
 *      String status,
 *      String message,
 *      T data,
 *   ) {}
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
     * 문제 : HTTP 메서드 POST에 맞지 않는 서비스 로직
     * 원인 : 데이터를 RequestBody로 받기 위해 POST를 활용한 것을 보입니다. (GET의 경우, 캐시 및 보안 문제로 인해 Body를 권장하지 않습니다.)
     * 개선안 :
     *          특정 조건으로 조회하는 로직이기 떄문에 GET 메서드가 올바릅니다.
     *          물론 과거와 달리, GET 메서드 사용시, Body를 활용하는 것이 허용되었지만, 좋은 설계와는 거리가 멀다고 생각합니다.
     *
     *          -------------------------------------------------------------------------------------------
     *          HTTP 공식 문서
     *          The GET method means retrieve whatever information ([...]) is identified by the Request-UR
     *          -------------------------------------------------------------------------------------------
     *
     *          GET 요청시, 필요한 데이터의 경우 Request Parameter으로 담아서 요청 보내는 것을 권장드립니다.
     *
     *          public ResponseEntity<ProductListResponse> getProductListByCategory(
     *                  @RequestParam String category,
     *                  @RequestParam(defaultValue = "0") int page,
     *                  @RequestParam(defaultValue = "10") int size
     *          ){ ... }
     *
     *
     * 문제 : Controller단에서의 Page 인터페이스 사용
     * 원인 :
     * 개선안 :
     *         별도의 DTO 선안하기
     *         Page의 import문(org.springframework.data.domain.Page)을 살펴보면, data, 즉 Repository, persistence 단의 인터페이스임을 알 수 있습니다.
     *         해당 객체를 Controller 단에서 사용하게 되면, MVC(controller-service-domain-repository), 즉 계층 간의 경계가 모호해 집니다.
     *
     *         그래서 productService.getListByCategory(dto)의 반환 값를 위핸 별도의 DTO를 정의할 것을 권장드립니다.
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