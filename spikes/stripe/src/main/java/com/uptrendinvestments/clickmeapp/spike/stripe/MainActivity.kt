package com.uptrendinvestments.clickmeapp.spike.stripe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.PaymentConfiguration
import com.stripe.android.payments.paymentlauncher.rememberPaymentLauncher
import com.stripe.android.paymentsheet.PaymentSheet

/**
 * Throwaway spike (Phase 2, D-10). Delete this module after Phase 7 (Stripe) / Phase 9 (Agora) absorbs its findings.
 *
 * Proves two Stripe SDK paths: saving a card through PaymentSheet in SetupIntent mode, and completing a 3DS challenge
 * with PaymentLauncher on the expert's connected account (Path A: the server confirms, the app never confirms the
 * PaymentIntent itself). Evidence is logged under the logcat tag StripeSpike.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PaymentConfiguration.init(applicationContext, BuildConfig.STRIPE_PUBLISHABLE_KEY)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SpikeScreen()
                }
            }
        }
    }
}

@Composable
private fun SpikeScreen(viewModel: StripeSpikeViewModel = viewModel()) {
    val sheet = remember { PaymentSheet.Builder(viewModel::onSetupResult) }.build()

    // stripeAccountId is a remember key inside rememberPaymentLauncher, so the challenge is set in state first and the
    // launcher is driven from LaunchedEffect, never from the same click handler.
    val launcher =
        rememberPaymentLauncher(
            BuildConfig.STRIPE_PUBLISHABLE_KEY,
            viewModel.challenge?.stripeAccount,
            viewModel::onPaymentResult,
        )

    LaunchedEffect(viewModel.setupRequest) {
        viewModel.setupRequest?.let { request ->
            sheet.presentWithSetupIntent(
                request.clientSecret,
                PaymentSheet.Configuration
                    .Builder("ClickMe")
                    .customer(PaymentSheet.CustomerConfiguration(request.customerId, request.ephemeralKey))
                    .build(),
            )
            viewModel.consumeSetupRequest()
        }
    }

    LaunchedEffect(viewModel.challenge) {
        viewModel.challenge?.let { launcher.handleNextActionForPaymentIntent(it.clientSecret) }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        LoginSection(viewModel)
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("Save card", style = MaterialTheme.typography.titleMedium)
        Button(onClick = viewModel::createSetupIntent, enabled = viewModel.token != null) {
            Text("Add card")
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        CardsSection(viewModel)
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        BookSection(viewModel)
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("Log", style = MaterialTheme.typography.titleMedium)
        viewModel.logLines.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun LoginSection(viewModel: StripeSpikeViewModel) {
    var email by remember { mutableStateOf(BuildConfig.CLIENT_EMAIL) }
    var password by remember { mutableStateOf("") }
    Text("Login", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email") },
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )
    Button(onClick = { viewModel.login(email, password) }) {
        Text("Log in")
    }
    Text(if (viewModel.token != null) "Logged in" else "Not logged in")
}

@Composable
private fun CardsSection(viewModel: StripeSpikeViewModel) {
    Text("Cards", style = MaterialTheme.typography.titleMedium)
    Button(onClick = viewModel::loadPaymentMethods, enabled = viewModel.token != null) {
        Text("Load cards")
    }
    viewModel.cards.forEach { card ->
        val marker = if (card.id == viewModel.selectedPm) "[x]" else "[ ]"
        val suffix = if (card.isDefault) " (default)" else ""
        OutlinedButton(onClick = { viewModel.selectCard(card.id) }) {
            Text("$marker ${card.brand} •••• ${card.last4}$suffix")
        }
    }
}

@Composable
private fun BookSection(viewModel: StripeSpikeViewModel) {
    Text("Book", style = MaterialTheme.typography.titleMedium)
    BookField("Expert id", viewModel.expertId) { viewModel.expertId = it }
    BookField("Topic id", viewModel.topicId) { viewModel.topicId = it }
    BookField("Selected date (UTC yyyy-MM-dd)", viewModel.selectedDate) { viewModel.selectedDate = it }
    BookField("Time slot (UTC HH:mm)", viewModel.timeSlot) { viewModel.timeSlot = it }
    BookField("Start time (ISO-8601)", viewModel.startTime) { viewModel.startTime = it }
    BookField("Duration minutes", viewModel.durationMinutes) { viewModel.durationMinutes = it }
    Button(onClick = viewModel::book, enabled = viewModel.token != null) {
        Text("Book with saved card")
    }
}

@Composable
private fun BookField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
    )
}
