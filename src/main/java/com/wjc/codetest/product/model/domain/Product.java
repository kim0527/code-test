package com.wjc.codetest.product.model.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/*
 * 문제 : 엔티티에서의 Public Setter 적용
 * 원인 : Update 구문을 편리하게 사용하기 위해 Lombok의 @Setter를 사용한 것으로 추측
 * 개선안 :
 *          1. @Setter 제거 와 별도의 Update 메서드 정의
 *
 *          엔티티에서 Lombok의 @Setter를 사용하게 되면 아래와 같음 문제점이 존재합니다.
 *
 *          첫번째, 코드의 의도가 파악이 어려워져 코드의 가독성이 떨어집니다.
 *          [예시 코드]
 *          Product product = new Product();
 *          product.setCategory("책");
 *          product.setName("이펙티브 자바");
 *
 *          위 코드를 살펴보면, Product 생성 코드 인지, Product 업데이트 코드 인지 의도륵 파악하기 어렵게 됩니다.
 *
 *          두번째, set 메서드가 public으로 설정되어, 외부에서 엔티티의 값을 자유롭게 변경할 수 있게 됩니다.
 *          Lombok의 set 메서드에 별도의 유효성 검사 로직을 추가할 수 없기 때문에, 엔티티의 데이터의 일관성을 무너트릴 수 있습니다.
 *
 *          그래서 @Setter를 제거하고, 별도의 Update 메서드 정의하시는 것을 권장드립니다.
 *
 *          public void update(String category, String name){
 *              validateCategory(category);
 *              validateName(name);
 *              this.category = category;
 *              this.name = name;
 *          }
 *
 * 문제 : Product 엔티티와 관련된 부가 정보(createdAt, createdBy, updatedAt, updatedBy ...) 부재로 인해, 문제 발생시 추후 데이터 변경 이력 추적에 어려움이 발생할 수 있습니다.
 * 원인 :
 * 개선안 :
 *          아래와 같이 엔티티의 변경 이력을 저장하는 Auditing용 엔티티를 정의하여 상속하도록 하면, Spring data Audit 기능을 활용할 수 있는 뿐만 아니라, 생성 및 상태 변경 이력도 확인할 수 있습니다.
 *          [예시 코드]
 *          @Getter
 *          @MappedSuperclass
 *          @EntityListeners(AuditingEntityListener.class)
 *          public class AuditableEntity {
 *
 *              @CreatedDate
 *              @Column(updatable = false)
 *              private LocalDateTime createdAt;
 *
 *              @LastModifiedDate
 *              private LocalDateTime updatedAt;
 *
 *              ...
 *          }
 *
 *          @Entity
 *          public class Product extends AuditableEntity { ... }
 *
 */
@Entity
@Getter
@Setter
public class Product {

    /*
     * 문제 : 엔티티 PK 전략으로 Long 채택
     *       정수로 PK 설정시 문제점
     *       1) 보안 문제
     *          숫자를 사용하기 때문에, PK를 유추하기 쉬워집니다. (SQL injection에 취약)
     *       2) 분산형 시스템에서의 PK 중복 발생
     *          DB 스케일 아웃 시, 데이터 베이스 끼리의 PK 중복이 발생할 수 있습니다.
     * 원인 :
     * 개선안 :
     *          1. UUID와 같은 Unique 한 String으로 ID 정의하기
     *              [예시 코드]
     *              @Id
     *              @UuidGenerator
     *              @Column(name = "product_id")
     *              @JdbcTypeCode(Types.VARCHAR)
     *              @Column(length = 36, nullable = false, unique = true)
     *              private UUID id;
     *
     *          트레이드 오프 - 1) 테스트 시 어려움, 2) String이기 때문에 조회 및 정렬 시에는 Long에 유리합니다.
     */
    @Id
    @Column(name = "product_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /*
     * 문제 : category 타입 단순 String 정의
     *       별도의 category를 조회하는 API가 있는 만큼, category는 중요한 도메인 정보로 추측됩니다.
     *       category를 단순 string으로 관리 시, 여러 문제 점 발생합니다.
     *       1) 카테고리 이름 변경 시, 기존 Product 엔티티에 저장된 category 값 일관성 유지가 어려움
     *       2) category validation 체크 불가 (요청 받은 category가 유효한 카테고리인지)
     * 원인 :
     * 개선안 :
     *          1. 별도의 Category 도메인 생성
     *             별도의 Category 도메인을 정의하여, 도메인 정보를 저장하고 이를 코드로써 관리합니다.
     *             Product에서는 CategoryCode를 가지고 있는 구조로 변경
     *
     *             [예시 코드]
     *             @Entity
     *             public class Category {            // Category 도메인 정의
     *                 @Id
     *                 private String Code;
     *                 private String name;
     *                 ...
     *             }
     *
     *             @Entity
     *             public class Product {
     *                 ...
     *
     *                 private String CategoryCode;    // CategoryCode 필드 정의
     *
     *                 ...
     *             }
     *
     */
    @Column(name = "category")
    private String category;

    @Column(name = "name")
    private String name;

    protected Product() {
    }

    /*
     * 문제 : 엔티티에서의 Public 생성자 사용
     * 원인 :
     * 개선안 :
     *          1. private으로 생성자 제한 및 정적 팩토리 메서드 패턴 사용
     *          외부에서 new로 객체를 무문별하게 생성하는 것을 방지하기 위해서는 생성자를 private로 제한할 필요가 있습니다.
     *          대신에, public static으로 엔티티 생성 전용 메서드를 정의한다. 이렇게 되면, 생성자를 private으로 제한하는 동시에, 생성에 대한 책임을 엔티티에게만 제공할 수 있습니다.
     *          [예시 코드]
     *          private Product(String category, String name) {
     *              this.category = category;
     *              this.name = name;
     *          }
     *          public static Product of(String name, String category) {
     *              return new Product(name, category);
     *          }
     *
     *          + 빌더 패턴 사용하기 (@Builder)
     *          + 롬복 활용하기(@AllArgsConstructor(access = AccessLevel.PRIVATE), @NoArgsConstructor(access = AccessLevel.PROTECTED))
     */
    public Product(String category, String name) {
        this.category = category;
        this.name = name;
    }

    /*
     * 문제 : 중복된 Get 메서드
     * 원인 :
     * 개선안 :
     *          1. getCategory(), getName() 제거
     *          @Getter를 통해 이미 제공하고 있기 떄문에, 중복된 메서드이므로 제거
     *
     */

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }
}
