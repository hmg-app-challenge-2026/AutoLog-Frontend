package kr.autolog.vehicle.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import kr.autolog.vehicle.R
import kr.autolog.vehicle.data.repository.AddDriverResult
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalWindowLayout
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.WindowLayout
import kr.autolog.vehicle.ui.design.components.AvatarFace
import kr.autolog.vehicle.ui.design.components.IconAvatar
import kr.autolog.vehicle.ui.design.components.InputField
import kr.autolog.vehicle.ui.design.components.ModalCard
import kr.autolog.vehicle.ui.design.components.PrimaryButton
import kr.autolog.vehicle.ui.design.components.TertiaryButton
import kr.autolog.vehicle.ui.design.components.initialsOf

/**
 * 스펙 7.1 [+ 새 운전자 추가] 팝업(F-24, origin=LOCAL). 가운데 카드(`ModalCard`).
 * 머리(아바타 미리보기 · 제목 · 한 줄 설명, 가운데 정렬) · 입력(이름 · 부서) · 안내 메시지(가운데, 버튼 바로 위) ·
 * [추가 및 시작하기](주 버튼, 전폭) · 그 아래 작은 글자 [취소].
 * WIDE(가로 차량 화면)는 높이가 640 dp라 한 줄로 쌓으면 버튼이 밀린다 — 왼쪽 머리 / 오른쪽 입력·버튼 두 칸.
 * TALL은 한 줄로 쌓고 버튼은 카드 하단에 고정한다(내용만 스크롤).
 * NARROW(분할 화면 한 칸, 504×640)는 머리를 제목만 남긴다 — 아바타·설명까지 두면 입력 칸이 스크롤 밖으로 밀렸다.
 */
@Composable
fun AddDriverDialog(form: AddDriverForm, onAction: (ProfilePickerAction) -> Unit, modifier: Modifier = Modifier) {
    if (LocalWindowLayout.current == WindowLayout.WIDE) {
        ModalCard(modifier, maxWidth = Size.sheetMaxWidth) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Space.s7),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AddDriverHeader(form.name, Modifier.weight(HEADER_WEIGHT))
                Column(Modifier.weight(1f - HEADER_WEIGHT)) {
                    AddDriverFields(form, onAction)
                    Spacer(Modifier.height(Space.s5))
                    AddDriverButtons(form, onAction)
                }
            }
        }
    } else {
        ModalCard(modifier, footer = { AddDriverButtons(form, onAction) }) {
            AddDriverHeader(
                form.name,
                Modifier.fillMaxWidth(),
                compact =
                LocalWindowLayout.current == WindowLayout.NARROW,
            )
            Spacer(Modifier.height(Space.s6))
            AddDriverFields(form, onAction)
        }
    }
}

/** WIDE 두 칸에서 머리 칸 폭 비율. */
private const val HEADER_WEIGHT = 0.4f

@Composable
private fun AddDriverHeader(name: String, modifier: Modifier, compact: Boolean = false) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (!compact) {
            NewDriverAvatar(name)
            Spacer(Modifier.height(Space.s5))
        }
        Text(
            stringResource(R.string.picker_add),
            style = t.headline,
            color = c.textPrimary,
            textAlign = TextAlign.Center,
        )
        if (!compact) {
            Spacer(Modifier.height(Space.s2))
            Text(
                stringResource(R.string.picker_add_subtitle),
                style = t.body,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AddDriverFields(form: AddDriverForm, onAction: (ProfilePickerAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.s5)) {
        InputField(
            label = stringResource(R.string.picker_add_name),
            value = form.name,
            onValueChange = { onAction(ProfilePickerAction.AddNameChanged(it)) },
            enabled = !form.saving,
            placeholder = stringResource(R.string.picker_add_name_hint),
            modifier = Modifier.fillMaxWidth(),
        )
        InputField(
            label = stringResource(R.string.picker_add_dept),
            value = form.dept,
            onValueChange = { onAction(ProfilePickerAction.AddDeptChanged(it)) },
            enabled = !form.saving,
            placeholder = stringResource(R.string.picker_add_dept_hint),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 안내 메시지(가운데) + 주 버튼 전폭 + 그 아래 작은 글자 [취소]. 취소는 기본 동작(추가)과 무게를 겹치지 않는다.
 * 안내 메시지를 버튼 바로 위(고정 영역)에 두는 이유: 좁은 창에서 입력 칸 아래에 두면 스크롤 밖에 숨어 누른 결과가 안 보였다.
 */
@Composable
private fun AddDriverButtons(form: AddDriverForm, onAction: (ProfilePickerAction) -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        form.error?.let {
            Text(
                errorText(it),
                style = AutoLogTheme.type.body,
                color = AutoLogTheme.colors.danger,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = Space.s4),
            )
        }
        PrimaryButton(
            stringResource(R.string.picker_add_confirm),
            onClick = { onAction(ProfilePickerAction.SubmitAdd) },
            modifier = Modifier.fillMaxWidth(),
            height = Size.buttonMd,
            enabled = form.canSubmit,
            loading = form.saving,
        )
        TertiaryButton(stringResource(R.string.action_cancel), onClick = { onAction(ProfilePickerAction.CancelAdd) })
    }
}

/** 이름이 비면 사람+ 아이콘, 치기 시작하면 이니셜. 아직 id가 없어 사람별 색 대신 accent 그라데이션(새 사람 표시). */
@Composable
private fun NewDriverAvatar(name: String) {
    val c = AutoLogTheme.colors
    if (name.isBlank()) {
        IconAvatar(Icons.Rounded.PersonAdd, Size.avatarLarge, iconSize = Size.iconLg)
    } else {
        AvatarFace(initialsOf(name), Brush.linearGradient(listOf(c.accent, c.accentFill)), Size.avatarLarge)
    }
}

@Composable
private fun errorText(failure: AddDriverResult.Failure): String = when (failure) {
    AddDriverResult.EmptyName -> stringResource(R.string.picker_add_name_required)
    AddDriverResult.Duplicate -> stringResource(R.string.picker_add_duplicate)
    is AddDriverResult.Rejected -> failure.reason
}
