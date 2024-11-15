// PaymentSuccessScreen.kt
package org.monerokon.xmrpos.ui.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun PaymentSuccessScreenRoot(viewModel: PaymentSuccessViewModel, navController: NavHostController) {
    viewModel.setNavController(navController)
    PaymentSuccessScreen(
        navigateToEntry = viewModel::navigateToEntry,
    )
}

@Composable
fun PaymentSuccessScreen(
    navigateToEntry: () -> Unit,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column (
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(innerPadding).padding(horizontal = 48.dp).fillMaxSize(),
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Done,
                    contentDescription = "Payment successful",
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Payment successful",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(32.dp))
            FilledTonalButton(
                onClick = {}
            ) {Text("Print receipt")}
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {navigateToEntry}
            ) {
                Text("Next order")
            }
        }
    }
}
