// バックエンドの DTO に対応する型。com.example.wanteat.dto と対で維持すること。

export type DishCategory = "MAIN" | "SIDE";
export type MenuStatus = "GENERATING" | "CONFIRMED" | "COOKED" | "FAILED";
export type RequestedBy = "SELF" | "PARTNER";
export type RequestStatus = "OPEN" | "FULFILLED";

/** 第1段階の提案（1品分）。分量と手順はまだ含まない。 */
export type SuggestedDish = {
  name: string;
  description: string | null;
  cookingMinutes: number | null;
  mainIngredients: string[] | null;
};

export type SuggestResponse = {
  mainDish: SuggestedDish;
  sideDish: SuggestedDish;
};

export type SuggestRequest = {
  maxCookingMinutes?: number | null;
  ingredients?: string | null;
  mood?: string | null;
  excludeDishNames?: string[];
};

export type Ingredient = {
  id: number;
  name: string;
  amount: string | null;
  unit: string | null;
  /** 常備品。レシピには出すが買い物リストには出ない。 */
  pantryStaple: boolean;
  sortOrder: number | null;
};

export type Dish = {
  id: number;
  category: DishCategory;
  name: string;
  description: string | null;
  cookingMinutes: number | null;
  steps: string[];
  sortOrder: number | null;
  ingredients: Ingredient[];
};

export type Menu = {
  id: number;
  cookedOn: string;
  status: MenuStatus;
  dishes: Dish[];
  createdAt: string;
  updatedAt: string;
};

export type ShoppingItem = {
  id: number;
  menuId: number | null;
  name: string;
  amount: string | null;
  unit: string | null;
  checked: boolean;
  sortOrder: number | null;
};

export type MealRequest = {
  id: number;
  requestedBy: RequestedBy;
  body: string;
  status: RequestStatus;
  fulfilledMenuId: number | null;
  createdAt: string;
  fulfilledAt: string | null;
};

export type Preference = {
  id: number;
  householdSize: number;
  allergies: string | null;
  dislikedFoods: string | null;
  note: string | null;
  updatedAt: string;
};
