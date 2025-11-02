package com.wjc.codetest.product.model.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/*
 * 문제 : 엔티티에 @Setter를 사용하고 있습니다.
 * 원인 :
 *       1. 개방-폐쇄 원칙에 위배되는 설계라고 생각합니다.
 *          @Setter 적용시, public set 메서드를 제공하게 됩니다.
 *          데이터 변경 로직이 public으로 공개되어, 외부에서 엔티티 필드의 값을 수정할 가능성을 제공합니다.
 *          수정은 폐쇄적이여야 한다는 OCP 원칙에 위배되며, 데이터 무결성 보장에 어려움이 생깁니다.
 *
 *        2. 필드값 validation이 어려워집니다.
 *           set 메서드는 보통 필드값 업데이트 시 사용되는 메서드인데, 데이터 변경시 변경되는 데이터의 유효성 검사 로직을 추가할 수 없게 됩니다.
 *
 *        3. 'set'이라는 명칭은 포괄적인 명칭이라 생각하기에, 명확한 메서드 역할 전달에 어려움을 준다고 생각합니다.
 *           자세한 메서드 명칭일 수록 코드의 가독성을 더욱 높일 수 있습니다.
 *
 * 개선안 :
 *         1. @Setter 제거 및 별도의 업데이트 메서드 정의를 권장드립니다.
 *            [예시 코드]
 *            public void update(String category, String name){
 *              validateCategory(category);
 *              validateName(name);
 *              this.category = category;
 *              this.name = name;
 *            }
 *
 * 문제 : Product 엔티티에 생성/수정/삭제 이력 정보 필드가 누락되어 있습니다.
 * 원인 :
 *       1. 추후 운영 관점에서, 유지보수 및 데이터 추적에 어려움이 발생할 수 있습니다.
 *          데이터 오염 등의 문제 발생시, 엔티티의 변경 이력은 운영 관점에서 필수적인 데이터 필드라 생각합니다.
 * 개선안 :
 *         1. Auditing 필드 추가 혹은 JPA Auditing 기능 활용을 권장드립니다.
 *            아래와 같이 엔티티의 변경 이력을 저장하는 Auditing용 엔티티를 정의하여 상속하도록 설계하면, 엔티티의 생성 및 상태 변경 이력을 자동을 생성/업데이트 할 수 있습니다.
 *            [예시 코드]
 *            @Getter
 *            @MappedSuperclass
 *            @EntityListeners(AuditingEntityListener.class)
 *            public class AuditableEntity {
 *
 *                @CreatedDate
 *                @Column(updatable = false)
 *                private LocalDateTime createdAt;
 *
 *                @LastModifiedDate
 *                private LocalDateTime updatedAt;
 *
 *                ...
 *            }
 *
 *            @Entity
 *            public class Product extends AuditableEntity { ... }
 *
 */
@Entity
@Getter
@Setter
public class Product {

    /*
     * 문제 : 엔티티의 PK가 Long 타입으로 설계 되어있습니다.
     * 원인 :
     *       Long 타입으로 PK를 설정시, 아래와 같은 단점이 존재합니다.
     *       1. PK가 정수이기 때문에, 유추 가능성이 존재하여 보안 문제가 발생할 수 있습니다.
     *          조회의 경우, url에 요청 파라미터로 조회하려는 엔티티의 id를 담는 경우가 많은데, 이떄 단순한 PK를 이용해 유추하여 다른 정보에 접근할 가능성이 존재합니다.
     *
     *       2. 분산형 시스템에서의 PK 값이 충돌할 가능성이 존재합니다.
     *          추후에 성능 개선을 위해 DB 스케일 아웃 시, 데이터 베이스 끼리의 정수 PK 값이 충돌할 수 있습니다.
     * 개선안 :
     *          1. UUID와 같은 Unique 한 String으로 Id를 정의하는 것을 권장드립니다.
     *              [예시 코드]
     *              @Id
     *              @UuidGenerator
     *              @Column(name = "product_id")
     *              @JdbcTypeCode(Types.VARCHAR)
     *              @Column(length = 36, nullable = false, unique = true)
     *              private UUID id;
     */
    @Id
    @Column(name = "product_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /*
     * 문제 : category 타입이 단순 String 정의되어 있습니다.
     * 원인 :
     *       category를 단순 string으로 관리는 아래와 같은 문제점이 있다고 생각합니다.
     *
     *       1. 카테고리 변경 시, 대규모 업데이트가 발생할 수 있습니다.
     *          특정 카테고리의 명칭이 변경되는 경우, 기존 카테고리 명칭을 가지고 있는 모든 데이터를 업데이트 해야하는 공수가 발생합니다.
     *          서비스적으로 업데이트 하는 것이 아닌 DB에 직접 쿼리로써 업데이트하는 방식은 데이터 무결성 측면에서 좋지 않은 방식이라 생각합니다.
     *       2. Category와 Product의 생명주기가 다르다고 생각합니다.
     *          별도의 category를 조회하는 API가 있는 만큼, category는 독립된 생명주기를 가지는 중요한 도메인 개념이라 추측됩니다.
     *          그리고 데이터 관점에서, 상품없이 카테고리가 존재할 수 있기 때문에 카테고리 안에 상품이 존재하는 구조로 카테고리가 더 넓은 생명주기를 가진다고 생각합니다.
     *
     * 개선안 :
     *          1. 별도의 Category 도메인을 생성할 것을 권장드립니다.
     *             별도의 Category 도메인을 정의하여, 도메인 정보를 저장하고 이를 코드로써 관리하는 방식입니다.
     *             Category가 별도의 엔티티로써 독립적인 생명주기를 가지게 되며, 유효한 카테고리 관리를 위한 유효성검사 로직 등의 카테고리 관련 로직을 분리할 수 있게 됩니다.
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
     * 문제 : 엔티티에서 public 생성자를 사용하고 있습니다.
     * 원인 :
     *        public 생성자를 통해 외부에서 new 키워드로 객체를 생성할 가능성을 제공합니다.
     *        현재 String에 대한 유효성 검사가 없는 점에서 유효하지 않은 값을 가진 객체 생성까지 초래할 수 있습니다.
     * 개선안 :
     *          1. private으로 생성자 제한 및 정적 팩토리 메서드 패턴 사용을 권장드립니다.
     *          외부에서 new로 객체를 무분별하게 생성하는 것을 방지하기 위해서는 생성자를 private로 제한할 필요가 있습니다.
     *          대신 public static 메서드를 통해 엔티티를 생성하도록 하면, 생성 책임을 엔티티 내부로 캡슐화함과 동시에 명확한 생성 의도를 표현할 수 있습니다.
     *          [예시 코드]
     *          private Product(String category, String name) {
     *              validate(category,name);
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
     * 문제 : getCategory(), getName() 메서드가 Lombok @Getter 로 이미 제공되는 기능과 중복됩니다.
     * 원인 : 의미적으로 동일한 메서드를 중복으로 정의하는 것은, 유지보수 측면서에 불필요한 코드 증가로 작용할 수 있습니다.
     * 개선안 :
     *          1. getCategory(), getName() 제거할 것을 권장드립니다.
     *          @Getter를 통해 제공함으로써, 더욱 간결한 코드를 작성할 수 있습니다.
     *
     */

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }
}
