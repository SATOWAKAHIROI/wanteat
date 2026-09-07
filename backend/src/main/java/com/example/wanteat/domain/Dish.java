package com.example.wanteat.domain;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dishes")
@Getter
@Setter
@NoArgsConstructor
public class Dish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DishCategory category;

    @Column(nullable = false, length = 100)
    private String name;

    /** ひとこと説明。20〜40 字程度。 */
    private String description;

    private Integer cookingMinutes;

    /** 調理手順。JSON の文字列配列として保持する。 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> steps = new ArrayList<>();

    private Integer sortOrder;

    @OneToMany(mappedBy = "dish", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Ingredient> ingredients = new ArrayList<>();

    /** 材料を追加する。関連の両側を同時にセットする。 */
    public void addIngredient(Ingredient ingredient) {
        ingredients.add(ingredient);
        ingredient.setDish(this);
    }
}
