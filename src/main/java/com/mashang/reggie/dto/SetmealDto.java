package com.mashang.reggie.dto;


import com.mashang.reggie.entity.Setmeal;
import com.mashang.reggie.entity.SetmealDish;
import lombok.Data;

import java.util.List;

@Data
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;

    private String categoryName;
}
