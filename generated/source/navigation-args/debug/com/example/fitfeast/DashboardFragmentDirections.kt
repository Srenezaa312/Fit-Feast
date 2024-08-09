package com.example.fitfeast

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections

public class DashboardFragmentDirections private constructor() {
  public companion object {
    public fun actionDashboardFragmentToProfileFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_dashboardFragment_to_profileFragment)

    public fun actionDashboardFragmentToWaterIntakeManagementFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_dashboardFragment_to_waterIntakeManagementFragment)

    public fun actionDashboardFragmentToMedicationFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_dashboardFragment_to_medicationFragment)

    public fun actionDashboardFragmentToWeightManagementFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_dashboardFragment_to_weightManagementFragment)

    public fun actionDashboardFragmentToCaloriesManagementFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_dashboardFragment_to_caloriesManagementFragment)
  }
}
