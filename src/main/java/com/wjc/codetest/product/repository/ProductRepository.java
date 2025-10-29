package com.wjc.codetest.product.repository;

import com.wjc.codetest.product.model.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findAllByCategory(String name, Pageable pageable);

    /*
     * 문제 : DISTINCT로 인하여 성능 저하 및 중복제거 실패 가능성이 존재합니다.
     * 원인 : [성능 저하]
     *       JPQL DISTINCT는 DB 레벨에서 중복 제거 연산을 수행한 후, 어플리케이션 단에서 엔티티 기준으로 중복 제거를 한번 더 수행합니다.
     *       여기서 DB 레벨에서 중복 제거 연산 수행 시, select된 컬럼의 전체 데이터를 불러와 중복 제거 연산을 수행합니다.
     *       즉, Product의 데이터 양이 많아질 수록, select된 컬럼의 전체 데이터를 불러오는 데에 사용되는 리소스 양이 증가하게 됩니다.
     *       [중복 제거 실패]
     *       category가 외부로부터 요청들어온 String 값이며, 별도의 validation이 없다는 가정하에, 띄어쓰기가 존재하는 category가 저장되는 경우 중복제거를 하지 못하게 됩니다.
     *       예시 ) "test", "test ", " test"
     * 개선안 :
     *          1. 별도의 Category 도메인 정의
     *          category 전용 도메인을 설계하게 되면, Product의 경우 카테고리 이름이 아닌 CategoryId를 가지게 되므로,
     *          정의되지 않는 다른 카테고리가 저장될 가능성이 없어지게 되며, 추가적으로 유효한 카테고리인지 validation도 가능해집니다.
     *          DISTINCT의 경우에도, 전체 데이터를 불러오는 리소스 양을 줄일 수 있습니다.
     *
     */
    @Query("SELECT DISTINCT p.category FROM Product p")
    List<String> findDistinctCategories();
}
