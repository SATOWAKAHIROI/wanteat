package com.example.wanteat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ingredients")
@Getter
@Setter
@NoArgsConstructor
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish;

    @Column(nullable = false, length = 100)
    private String name;

    /** 分量。「1/2」「大さじ2」など文字列で保持する。 */
    @Column(length = 50)
    private String amount;

    @Column(length = 20)
    private String unit;

    private Integer sortOrder;

    /** 水・塩・油など家に常備している材料。true なら買い物リストへ展開しない。 */
    @Column(nullable = false)
    private boolean pantryStaple;
}
