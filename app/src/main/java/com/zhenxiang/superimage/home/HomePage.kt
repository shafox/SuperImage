package com.zhenxiang.superimage.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.request.ImageRequest
import coil.transition.CrossfadeTransition
import com.zhenxiang.realesrgan.UpscalingModel
import com.zhenxiang.superimage.R
import com.zhenxiang.superimage.model.DataState
import com.zhenxiang.superimage.model.InputImage
import com.zhenxiang.superimage.model.OutputFormat
import com.zhenxiang.superimage.ui.form.MonoDropDownMenu
import com.zhenxiang.superimage.ui.mono.*
import com.zhenxiang.superimage.ui.theme.MonoTheme
import com.zhenxiang.superimage.ui.theme.border
import com.zhenxiang.superimage.ui.theme.spacing
import com.zhenxiang.superimage.ui.utils.RowSpacer
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(viewModel: HomePageViewModel) = Scaffold(
    topBar = { TopBar() }
) { padding ->

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let { viewModel.loadImage(it) }
    }
    val requestPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        LaunchedEffect(Unit) {
            if (
                ContextCompat.checkSelfPermission(
                    viewModel.getApplication(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Column(modifier = Modifier.padding(padding)) {

        val selectedImageState by viewModel.selectedImageFlow.collectAsStateWithLifecycle()

        ImagePreview(
            modifier = Modifier
                .weight(1f, true)
                .fillMaxWidth(),
            selectedImageState = selectedImageState,
            selectedModelState = viewModel.selectedUpscalingModelFlow.collectAsStateWithLifecycle(),
        ) { imagePicker.launch(HomePageViewModel.IMAGE_MIME_TYPE) }

        Options(
            upscalingModelFlow = viewModel.selectedUpscalingModelFlow,
            outputFormatFlow = viewModel.selectedOutputFormatFlow,
            selectedImageState = selectedImageState,
            onSelectImageClick = { imagePicker.launch(HomePageViewModel.IMAGE_MIME_TYPE) },
            onUpscaleClick = { viewModel.upscale() }
        )
    }
}

@Composable
private fun TopBar() {
    MonoAppBar(
        title = { Text(stringResource(id = R.string.app_name)) }
    )
}

@Composable
private fun ImagePreview(
    modifier: Modifier,
    selectedImageState: DataState<InputImage, Unit>?,
    selectedModelState: State<UpscalingModel>,
    onSelectImageClick: () -> Unit
) {

    val crossfadeTransition = remember { CrossfadeTransition.Factory(125) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (selectedImageState) {
            is DataState.Success -> selectedImageState.data.let {
                BlurShadowImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(it.tempFile)
                        .transitionFactory(crossfadeTransition)
                        .build(),
                    contentDescription = it.fileName,
                    modifier = Modifier.weight(1f, fill = false),
                    imageModifier = Modifier
                        .padding(
                            horizontal = MaterialTheme.spacing.level3,
                            vertical = MaterialTheme.spacing.level5,
                        )
                        .clip(MaterialTheme.shapes.large)
                )

                Text(
                    text = stringResource(id = R.string.original_image_resolution_label, it.width, it.height)
                )

                val selectedModel by selectedModelState
                Text(
                    modifier = Modifier.padding(bottom = MaterialTheme.spacing.level5),
                    text = stringResource(
                        id = R.string.output_image_resolution_label,
                        it.width * selectedModel.scale,
                        it.height * selectedModel.scale
                    )
                )
            }
            else -> StartWizard(onSelectImageClick)
        }
    }
}

@Composable
private fun ColumnScope.StartWizard(onSelectImageClick: () -> Unit) {
    Text(
        modifier = Modifier.padding(vertical = MaterialTheme.spacing.level5),
        text = stringResource(id = R.string.select_image_wizard_hint),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelLarge,
    )
    MonoButton(onClick = onSelectImageClick) {
        MonoButtonIcon(
            painterResource(id = R.drawable.ic_image_24),
            contentDescription = null
        )
        Text(
            stringResource(id = R.string.select_image_label)
        )
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun SetupWizardPreview() = MonoTheme {
    Scaffold {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.level5).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StartWizard { }
        }
    }
}

@Composable
private fun OutputFormatSelection(
    modifier: Modifier = Modifier,
    flow: MutableStateFlow<OutputFormat>
) {

    val selected by flow.collectAsStateWithLifecycle()

    MonoDropDownMenu(
        modifier = modifier,
        value = selected,
        label = { Text(stringResource(id = R.string.output_format_title)) },
        options = OutputFormat.VALUES,
        toStringAdapter = { it.formatName },
    ) {
        flow.tryEmit(it)
    }
}

@Composable
private fun ModelSelection(
    modifier: Modifier = Modifier,
    flow: MutableStateFlow<UpscalingModel>
) {

    val selected by flow.collectAsStateWithLifecycle()

    MonoDropDownMenu(
        modifier = modifier,
        value = selected,
        label = { Text(stringResource(id = R.string.selected_mode_label)) },
        options = UpscalingModel.VALUES,
        toStringAdapter = { stringResource(id = it.labelRes) },
    ) {
        flow.tryEmit(it)
    }
}

@Composable
private fun Options(
    upscalingModelFlow: MutableStateFlow<UpscalingModel>,
    outputFormatFlow: MutableStateFlow<OutputFormat>,
    selectedImageState: DataState<InputImage, Unit>?,
    onSelectImageClick: () -> Unit,
    onUpscaleClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .drawTopBorder(MaterialTheme.border.regular)
            .padding(
                horizontal = MaterialTheme.spacing.level3,
                vertical = MaterialTheme.spacing.level4
            )
    ) {
        Text(
            modifier = Modifier.padding(
                start = MaterialTheme.spacing.level3,
                end = MaterialTheme.spacing.level3,
                bottom = MaterialTheme.spacing.level4
            ),
            text = stringResource(id = R.string.upscaling_options_title),
            style = MaterialTheme.typography.headlineSmall
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModelSelection(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = MaterialTheme.spacing.level4),
                flow = upscalingModelFlow
            )
            OutputFormatSelection(
                modifier = Modifier.weight(1f),
                flow = outputFormatFlow
            )
        }

        val imageSelected = selectedImageState is DataState.Success

        Row(
            modifier = Modifier
                .padding(vertical = MaterialTheme.spacing.level5)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (imageSelected) {
                MonoButton(
                    modifier = Modifier.padding(end = MaterialTheme.spacing.level4),
                    onClick = onSelectImageClick
                ) {
                    MonoButtonIcon(
                        painterResource(id = R.drawable.ic_image_24),
                        contentDescription = null
                    )
                    Text(
                        stringResource(id = R.string.change_image_label)
                    )
                }   
            } else {
                RowSpacer()
            }

            MonoButton(
                enabled = imageSelected,
                onClick = onUpscaleClick,
            ) {
                MonoButtonIcon(
                    painterResource(id = R.drawable.outline_auto_awesome_24),
                    contentDescription = null
                )
                Text(stringResource(id = R.string.upscale_label))
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun OptionsPreview() = MonoTheme {
    Scaffold {
        Options(
            upscalingModelFlow = MutableStateFlow(UpscalingModel.X4_PLUS),
            outputFormatFlow = MutableStateFlow(OutputFormat.PNG),
            selectedImageState = DataState.Success(InputImage("", File(""), 0, 0)),
            onSelectImageClick = { },
            onUpscaleClick = { }
        )
    }
}
