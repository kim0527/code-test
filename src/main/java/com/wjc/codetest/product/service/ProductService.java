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
import org.springframework.transaction.annotation.Transactional;

/*
 * 문제 : update, delete 등 데이터의 변경이 발생하는 서비스 로직에 @Transactional가 설정되어 있지 않습니다.
 * 원인 :
 *       트랜잭션을 보장하지 않는다면, 아래와 같은 문제가 발생할 수 있습니다.
 *       1. 동작 의도와 다른 데이터 변경이 발생할 수 있습니다.
 *          service에서 여러 비즈니즈 로직 수행 중, 엔티티의 데이터가 변경된 상태에서 예외가 발생하게 되면
 *          엔티티는 롤백되지 않고, 변경된 상태가 그대로 반영되게 됩니다. 즉, 서비스 로직이 실패했음에도, 데이터에는 변경이 발생하는 문제가 발생합니다. (데이터 정합성 보장 X)
 *       2. 불안전한 영속성 컨텍스트 동작을 초래할 수 있습니다.
 *          영속성 컨텍스트의 생명주기는 일반적으로 트랜잭션 생명주기와 동욀하여, 트랜잭션 생성, 커밋 기준으로 동작합니다.
 *          @Transactional의 부재는 이러한 영속성 컨텍스 동작 불안전성을 제공할 뿐만 아니라, 변경감지와 같은 기능을 활용할 수 없게 됩니다.
 *
 * 개선안 :
 *          @Transactional과 @Transactional(readOnly=true)를 활용하는 것을 권장드립니다.
 *          최상단에 @Transactional(readOnly=true)를 설정하고 update, delete와 같이 데이터의 상태가 바뀌는 로직이 존재하는 경우,
 *          @Transactional를 설정함으로써 트랜잭션 롤백을 활용하여 데이터의 무결성을 보장할 수 있습니다.
 *
 *          추후에 Service를 데이터 변경 로직이 존재하는 CommandService(@Transactional), 조회 로직인 QueryService(@Transactional(readOnly=true))로 분리하는 방식도 활용 가능합니다.
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
     * 문제 : Optional을 반환함에도 불고하고, if문을 활용하여 예외처리하였습니다.
     * 원인 :
     *       Optional은 기존의 if문을 활용하여 객체의 null을 검사하는 방식의 문제점을 해소하기 위해 추가된 기능입니다.
     *       그래서 Repository에서 존재하지 않은 엔티티를 validation 하기 위해 많이 사용되고, getProductById()에서도 이를 확인하기 위해  productRepository에서 Optional을 반환했습니다.
     *
     *       하지만 이후 검증 로직은 Optional의 등장 배경과는 달리, if문을 활용하여 검증을 수행하고 있습니다.
     *       이러한 설계법은 Optional 기능 추가 의도와 다른 설계라고 생각합니다.
     *
     * 개선안 :
     *          1. Optional API를 적극 활용하여 코드의 간결성을 부여하는 것을 권장드립니다.
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
     * 문제 : 영속 상태의 엔티티를 수정한 이후, 불필요한 save()를 호출하고 있고, 불필요한 지역번수(updatedProduct)를 반환하고 있습니다.
     * 원인 :
     *        1. JPA 활용도 낮은 코드 작성이라고 생각합니다.
     *           JPA에서는 1차 캐시를 활용하여 영속 상태의 엔티티의 스냅샨을 저장하고,
     *           트랜잭션 커밋 시점에 변경이 존재하는 경우 자동으로 이를 감지하여 UPDATE를 해주는 기능이 있습니다. (변경감지, 더티체킹)
     *           그래서 별도의 save() 로직을 사용하지 않아도 변경 사항이 merge() 할 수 있습니다.
     *
     *         2. 불필요한 코드가 증가하여 코드의 가독성이 떨어진다고 생각합니다.
     *            update()에서 updatedProduct를 선언한 이유는 업데이트된 엔티티를 반환하려는 의도로 추측됩니다.
     *            하지만 Product product = getProductById(dto.getId());에서 반환하는 product는 영속 상태로 관리되고 있습니다.
     *            영속성 컨텍스트는 엔티티 인스턴트의 동일성을 보장하기 때문에, product를 반환하더라도 업데이트된 엔티티를 반환할 수 있습니다.
     * 개선안 :
     *         1. @Transactional 추가 후, 변경 감지를 활용하여 간결한 코드로 개선하는 것을 권장드립니다.
     *            (변경감지는 @Transactional이 필수입니다.)
     *         AS IS :
     *                  Product updateProduct = productRepository.save(product);
     *                  return updatedProduct;
     *         TO BE :
     *                  return product;
     *
     * 문제 : 엔티티 필드 업데이트를 set 메서드를 활용하여 수행하고 있습니다.
     * 원인 :
     *        현재 해당 메서드는 엔티티의 필드 값을 업데이트 할떄 사용되고 있습니다.
     *        하지만 set이라는 명칭은 "업데이트" 라는 기능적 의미를 전달하기에는 포괄적인 명칭이라고 생각합니다.
     *        이러한 상세한 메서드 명칭 정의는 여러 개발자들와 협업에서 코드 가독성 측면에서 필수적으로 수행되어야 한다고 생각합니다.
     * 개선안 :
     *         업데이트 라는 기능적 의미를 전달할 수 있도록 @Setter 사용이 아닌, 별도의 update 메서드를 정의하는 것을 권장드립니다.
     *         [예시 코드]
     *         public void updateCategory(String category){
     *              validateCategory(category);
     *              this.category = category;
     *         }
     */
    @Transactional
    public Product update(UpdateProductRequest dto) {
        Product product = getProductById(dto.getId());
        product.setCategory(dto.getCategory());
        product.setName(dto.getName());
        Product updatedProduct = productRepository.save(product);
        return updatedProduct;
    }

    /*
     * 문제 : product 삭제 로직을, 물리적 데이터 삭제 로직으로 구현하였습니다.
     * 원인 :
     *       엔티티 물리적 삭제는 아래와 같은 문제점이 발생할 수 있습니다.
     *       1. 데이터 복구가 필요한 경우 복구에 어려움이 있습니다.
     *          신규 요구사항 및 정책 밴경으로, 데이터 복구가 필요한 경우 데이터가 물리적으로 삭제되었기 때문에,
     *          DB 백업이 존재하지 않은 경우, 데이터 복구가 어려워질 수 있습니다.
     *       2. 의도치 않은 추가적인 데이터 삭제를 야기할 수 있습니다.
     *          만약 Product가 다른 엔티티와 연관관계를 가지고 있고, CASCADE 설정이 있다면, 의도치 않은 추가적인 데이터 삭제가 발생할 수 있습니다.
     * 개선안 :
     *         1. Soft Delete 방식으로 삭제 로직을 구현할 것을 권장드립니다.
     *            물리적 삭제가 아닌, 삭제를 나타는 필드를 통해 논리적 삭제를 수행하는 것을 말합니다.
     *            이후 특정 주기적 배치 작업을 통해 삭제 상태의 데이터를 대상으로 물리적 삭제를 수행할 수 있습니다.
     *            [예시 코드]
     *            @Entity
     *            public class product {
     *
     *                ...
     *
     *                @Column(name = "deleted")
     *                private Boolean deleted;
     *
     *                public void delete() {
     *                   validateDeleted();
     *                   this.deleted = Boolean.TRUE;
     *                }
     *             }
     */
    public void deleteById(Long productId) {
        Product product = getProductById(productId);
        productRepository.delete(product);
    }


    /*
     * 문제 : Serivce에서 Page 인터페이스를 반환하고 있습니다.
     * 원인 :
     *        Page의 import문(org.springframework.data.domain.Page)을 살펴보면, data, 즉 Repository, persistence 단의 인터페이스임을 알 수 있습니다.
     *        해당 객체를 Service에서 반환하게 되면, Controller에 spring.data 의존성이 발생합니다. MVC(controller-service-domain-repository), 즉 계층 간의 경계가 모호해 집니다.
     * 개선안 :
     *         별도의 Pagination 전용 DTO 선언하여 service에 전달하는 것을 권장드립니다.
     *         [예시 코드]
     *         @Getter
     *         @AllArgsConstructor(access = AccessLevel.PRIVATE)
     *         public class Pagination<T> {
     *             private final int page;
     *             private final int size;
     *             private final Long total;
     *             private final List<T> content;
     *
     *             public static <T> Pagination<T> of(int page, int size, Long total, List<T> content) {
     *                 return new Pagination<>(page, size, total, content);
     *             }
     *         }
     *
     *
     * 문제 : page, size에 대한 유효성 검증이 존재하지 않습니다.
     * 원인 :
     *      PageRequest의 생성자 내부 코드를 살펴보면, page는 0보다 작은 경우, size는 1보다 작은 경우 예외를 반환합니다.
     *      GetProductListRequest에 page는 음수, size는 0이 담겨 있을 경우, 런타임에러가 발생할 수 있습니다.
     *
     *      [AbstractPageRequest 생성자]
     *      public AbstractPageRequest(int pageNumber, int pageSize) {
     *           if (pageNumber < 0) {
     *             throw new IllegalArgumentException("Page index must not be less than zero");
     *           } else if (pageSize < 1) {
     *             throw new IllegalArgumentException("Page size must not be less than one");
     *           } else {
     *             this.pageNumber = pageNumber;
     *             this.pageSize = pageSize;
     *           }
     *       }
     */
    public Page<Product> getListByCategory(GetProductListRequest dto) {
        PageRequest pageRequest = PageRequest.of(dto.getPage(), dto.getSize(), Sort.by(Sort.Direction.ASC, "category"));
        return productRepository.findAllByCategory(dto.getCategory(), pageRequest);
    }

    public List<String> getUniqueCategories() {
        return productRepository.findDistinctCategories();
    }
}