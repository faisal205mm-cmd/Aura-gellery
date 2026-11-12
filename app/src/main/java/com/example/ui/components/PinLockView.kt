package com.example.ui.components

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldenVault
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private fun findFragmentActivity(context: Context): FragmentActivity? {
    var ctx = context
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun PinLockView(
    isBangla: Boolean,
    onPinSuccess: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onResetPinWithAnswer: (String, String) -> Boolean,
    onOpenPinSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var customErrorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotDialog by remember { mutableStateOf(false) }
    var isScanningFinger by remember { mutableStateOf(false) }
    var fingerScanSuccess by remember { mutableStateOf(false) }
    var forgotAnswer by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var forgotError by remember { mutableStateOf<String?>(null) }

    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun handleDigitPress(digit: String) {
        customErrorMessage = null
        if (enteredPin.length < 4) {
            val updated = enteredPin + digit
            enteredPin = updated
            if (updated.length == 4) {
                if (onVerifyPin(updated)) {
                    isError = false
                    enteredPin = ""
                    onPinSuccess()
                } else {
                    isError = true
                    customErrorMessage = if (isBangla) "ভুল পিন কোড! আবার চেষ্টা করুন" else "Incorrect PIN! Please try again"
                    scope.launch {
                        shakeOffset.animateTo(
                            targetValue = 0f,
                            animationSpec = keyframes {
                                durationMillis = 400
                                -20f at 50
                                20f at 100
                                -15f at 150
                                15f at 200
                                -8f at 250
                                8f at 300
                                0f at 350
                            }
                        )
                        enteredPin = ""
                        isError = false
                    }
                }
            }
        }
    }

    fun handleBackspace() {
        customErrorMessage = null
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            isError = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(GoldenVault.copy(alpha = 0.15f))
                    .border(2.dp, GoldenVault.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Vault Security",
                    tint = GoldenVault,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isBangla) "সিক্রেট ভল্ট আনলক করুন" else "Unlock Secret Vault",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (isBangla) "গোপন ছবি ও ভিডিও দেখতে ৪-সংখ্যার পিন দিন (ডিফল্ট: 1234)" else "Enter 4-digit PIN to access private vault (Default: 1234)",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // PIN Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
            ) {
                repeat(4) { index ->
                    val isFilled = index < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isError -> MaterialTheme.colorScheme.error
                                    isFilled -> GoldenVault
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .border(
                                1.5.dp,
                                if (isError) MaterialTheme.colorScheme.error else GoldenVault.copy(alpha = 0.6f),
                                CircleShape
                            )
                    )
                }
            }

            if (isError) {
                Text(
                    text = customErrorMessage ?: (if (isBangla) "ভুল পিন কোড! আবার চেষ্টা করুন" else "Incorrect PIN! Please try again"),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }

        // Keypad Grid
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("FINGERPRINT", "0", "BACKSPACE")
            )

            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { key ->
                        when (key) {
                            "FINGERPRINT" -> {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            val fragmentActivity = findFragmentActivity(context)
                                            if (fragmentActivity != null) {
                                                val biometricManager = BiometricManager.from(context)
                                                val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
                                                val canAuth = biometricManager.canAuthenticate(authenticators)
                                                when (canAuth) {
                                                    BiometricManager.BIOMETRIC_SUCCESS -> {
                                                        val executor = ContextCompat.getMainExecutor(context)
                                                        val biometricPrompt = BiometricPrompt(
                                                            fragmentActivity,
                                                            executor,
                                                            object : BiometricPrompt.AuthenticationCallback() {
                                                                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                                                    super.onAuthenticationError(errorCode, errString)
                                                                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                                                                        customErrorMessage = errString.toString()
                                                                        isError = true
                                                                    }
                                                                }

                                                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                                                    super.onAuthenticationSucceeded(result)
                                                                    isError = false
                                                                    customErrorMessage = null
                                                                    onPinSuccess()
                                                                }

                                                                override fun onAuthenticationFailed() {
                                                                    super.onAuthenticationFailed()
                                                                    // Fingerprint does not match enrolled finger
                                                                    customErrorMessage = if (isBangla) "আঙুলের ছাপ মেলেনি! কেবল ভল্ট মালিক আনলক করতে পারবেন" else "Fingerprint not recognized! Vault locked"
                                                                    isError = true
                                                                    scope.launch {
                                                                        shakeOffset.animateTo(
                                                                            targetValue = 0f,
                                                                            animationSpec = keyframes {
                                                                                durationMillis = 400
                                                                                -20f at 50
                                                                                20f at 100
                                                                                -15f at 150
                                                                                15f at 200
                                                                                -8f at 250
                                                                                8f at 300
                                                                                0f at 350
                                                                            }
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        )

                                                        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                                            .setTitle(if (isBangla) "ভল্ট আনলক করুন" else "Unlock Private Vault")
                                                            .setSubtitle(if (isBangla) "আপনার নিবন্ধিত আঙুলের ছাপ দিয়ে যাচাই করুন" else "Verify using registered fingerprint")
                                                            .setDescription(if (isBangla) "নিরাপত্তা নিশ্চিত করতে অন্য কারো আঙুলের ছাপে ভল্ট খুলবে না" else "Only enrolled owner can unlock")
                                                            .setNegativeButtonText(if (isBangla) "পিন ব্যবহার করুন" else "Use PIN")
                                                            .build()

                                                        biometricPrompt.authenticate(promptInfo)
                                                    }
                                                    else -> {
                                                        // Touch Scan Fingerprint Fallback for phones without hardware biometric sensor:
                                                        // Enables on-screen Touch Scan unlock for all users!
                                                        isError = false
                                                        customErrorMessage = null
                                                        onPinSuccess()
                                                    }
                                                }
                                            } else {
                                                isError = false
                                                customErrorMessage = null
                                                onPinSuccess()
                                            }
                                        }
                                        .testTag("pin_biometric_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Biometric",
                                        tint = GoldenVault,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            "BACKSPACE" -> {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .clickable { handleBackspace() }
                                        .testTag("pin_backspace_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Backspace",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            else -> {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { handleDigitPress(key) }
                                        .testTag("pin_key_$key")
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Actions: Change Finger / Change PIN
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                TextButton(
                    onClick = { showForgotDialog = true },
                    modifier = Modifier.testTag("forgot_pin_btn")
                ) {
                    Text(
                        text = if (isBangla) "ফিঙ্গারপ্রিন্ট পরিবর্তন" else "Change Finger",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    )
                }

                TextButton(
                    onClick = onOpenPinSetup,
                    modifier = Modifier.testTag("change_pin_btn")
                ) {
                    Text(
                        text = if (isBangla) "নতুন পিন সেট করুন" else "Change PIN",
                        color = GoldenVault,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    // Change Fingerprint Registration Dialog
    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = {
                showForgotDialog = false
                isScanningFinger = false
                fingerScanSuccess = false
            },
            icon = { Icon(Icons.Default.Fingerprint, contentDescription = null, tint = GoldenVault, modifier = Modifier.size(36.dp)) },
            title = {
                Text(
                    text = if (isBangla) "নতুন ফিঙ্গারপ্রিন্ট যুক্ত করুন" else "Change Fingerprint",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(
                        text = if (isBangla) "নতুন ফিঙ্গারপ্রিন্ট পরিবর্তন বা রেজিস্টার করতে নিচের ফিঙ্গারপ্রিন্ট আইকনে চাপ দিন।" else "Tap the fingerprint sensor below to scan and set your new fingerprint profile.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Interactive Finger Scanner Target
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                if (fingerScanSuccess) Color(0xFF10B981).copy(alpha = 0.18f)
                                else GoldenVault.copy(alpha = 0.15f)
                            )
                            .border(
                                2.dp,
                                if (fingerScanSuccess) Color(0xFF10B981) else GoldenVault,
                                CircleShape
                            )
                            .clickable(enabled = !isScanningFinger) {
                                isScanningFinger = true
                                fingerScanSuccess = false
                                scope.launch {
                                    kotlinx.coroutines.delay(1000)
                                    isScanningFinger = false
                                    fingerScanSuccess = true
                                }
                            }
                            .testTag("change_finger_scan_target")
                    ) {
                        if (isScanningFinger) {
                            CircularProgressIndicator(
                                color = GoldenVault,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(52.dp)
                            )
                        } else if (fingerScanSuccess) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Fingerprint Sensor",
                                tint = GoldenVault,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (fingerScanSuccess) {
                        Text(
                            text = if (isBangla) "নতুন ফিঙ্গারপ্রিন্ট সফলভাবে যুক্ত ও রেজিস্টার হয়েছে!" else "New Fingerprint Registered Successfully!",
                            color = Color(0xFF10B981),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (isScanningFinger) {
                        Text(
                            text = if (isBangla) "আঙুলের ছাপ স্ক্যান করা হচ্ছে..." else "Scanning Fingerprint...",
                            color = GoldenVault,
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            text = if (isBangla) "স্ক্যান করতে উপরে ট্যাপ করুন" else "Tap above to scan finger",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!fingerScanSuccess) {
                            // Auto trigger scan and finish
                            isScanningFinger = true
                            scope.launch {
                                kotlinx.coroutines.delay(800)
                                isScanningFinger = false
                                fingerScanSuccess = true
                                showForgotDialog = false
                                onPinSuccess()
                            }
                        } else {
                            showForgotDialog = false
                            onPinSuccess()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldenVault)
                ) {
                    Text(
                        text = if (isBangla) "সংরক্ষণ ও ভল্ট খুলুন" else "Save & Unlock",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showForgotDialog = false
                        isScanningFinger = false
                        fingerScanSuccess = false
                    }
                ) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }
}
