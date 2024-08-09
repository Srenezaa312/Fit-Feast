package com.example.fitfeast

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections

public class FragmentAddWeightDirections private constructor() {
  public companion object {
    public fun actionFragmentAddWeightToWeightManagementFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_fragmentAddWeight_to_weightManagementFragment)
  }
}
