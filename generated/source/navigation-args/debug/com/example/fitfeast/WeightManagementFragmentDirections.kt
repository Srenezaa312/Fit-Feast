package com.example.fitfeast

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections

public class WeightManagementFragmentDirections private constructor() {
  public companion object {
    public fun actionWeightManagementFragmentToFragmentAddWeight(): NavDirections =
        ActionOnlyNavDirections(R.id.action_weightManagementFragment_to_fragmentAddWeight)
  }
}
