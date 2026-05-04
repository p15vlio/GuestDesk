package net.kustax.opendelivery.web

sealed class Screen {
    object Login : Screen()
    object Dashboard : Screen() // This will be the OWNER dashboard (empty for now)
    object AdminDashboard : Screen()
    object OwnerList : Screen()
    object AdminProperties : Screen()
    object AdminProducts : Screen()
}
