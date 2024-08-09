package com.example.fitfeast

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections

public class UserProfileCreationFragmentDirections private constructor() {
  public companion object {
    public fun actionUserProfileCreationFragmentToDashboardFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_userProfileCreationFragment_to_dashboardFragment)
  }
}
