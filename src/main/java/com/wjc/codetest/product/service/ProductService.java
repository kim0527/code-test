package com.wjc.codetest.product.service;

import com.wjc.codetest.product.model.request.CreateProductRequest;
import com.wjc.codetest.product.model.request.GetProductListRequest;
import com.wjc.codetest.product.model.domain.Product;
import com.wjc.codetest.product.model.request.UpdateProductRequest;
import com.wjc.codetest.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

/*
 * 문제 : 서버 상태가 불안전할 시, 원자성을 보장하지 않아, 데이터 무결성이 깨질 수 있습니다.
 * 원인 : @Transactional의 부재
 * 개선안 :
 *          @Transactional과 @Transactional(readOnly=true) 활용하기
 *          최상단에 @Transactional(readOnly=true)를 설정하고 update, delete와 같이 데이터의 상태가 바뀌는 로직이 존재하는 경우,
 *          @Transactional를 설정함으로써 트랜잭션 롤백을 활용하여 데이터의 무결성을 보장할 수 있습니다.
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Product create(CreateProductRequest dto) {
        Product product = new Product(dto.getCategory(), dto.getName());
        return productRepository.save(product);
    }

    /*
     * 문제 : 코드의 depth 증가하였습니다.
     * 원인 : if문 사용
     * 개선안 :
     *          JDK 17이기 때문에 Optional API를 활용하면 더욱 간결한 코드를 작성할 수 있습니다.
     *          AS IS :
     *                  Optional<Product> productOptional = productRepository.findById(productId);
     *                  if (!productOptional.isPresent()) {
     *                      throw new RuntimeException("product not found");
     *                  }
     *                  return productOptional.get();
     *          TO BE :
     *                  return productRepository.findById(productId).orElseThrow(() -> new RuntimeException("product not found"));
     *
     */
    public Product getProductById(Long productId) {
        Optional<Product> productOptional = productRepository.findById(productId);
        if (!productOptional.isPresent()) {
            throw new RuntimeException("product not found");
        }
        return productOptional.get();
    }

    /*
     * 문제 : 불필요한 지역 변수 updatedProduct 선언
     * 원인 :
     * 개선안 :
     *         변수 선언 없이 바로 return
     *         AS IS :
     *                  Product updateProduct = productRepository.save(product);
     *                  return updatedProduct;
     *         TO BE :
     *                  return productRepository.save(product);
     *
     * 문제 : set이라는 불명확한 메서드 이름
     * 원인 :
     * 개선안 :
     *         update 이름을 사용하여, 더욱 명확한 메서드 정의 및 표현을 권장드립니다.
     */
    public Product update(UpdateProductRequest dto) {
        Product product = getProductById(dto.getId());
        product.setCategory(dto.getCategory());
        product.setName(dto.getName());
        Product updatedProduct = productRepository.save(product);
        return updatedProduct;

    }

    public void deleteById(Long productId) {
        Product product = getProductById(productId);
        productRepository.delete(product);
    }

    public Page<Product> getListByCategory(GetProductListRequest dto) {
        PageRequest pageRequest = PageRequest.of(dto.getPage(), dto.getSize(), Sort.by(Sort.Direction.ASC, "category"));
        return productRepository.findAllByCategory(dto.getCategory(), pageRequest);
    }

    public List<String> getUniqueCategories() {
        return productRepository.findDistinctCategories();
    }
}