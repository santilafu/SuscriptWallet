package com.subia.shared

import com.subia.shared.model.GmailAddResult
import com.subia.shared.model.GmailDetected
import com.subia.shared.model.GmailScanTicketResponse
import com.subia.shared.repository.GmailScanRepository
import com.subia.shared.viewmodel.GmailScanUiState
import com.subia.shared.viewmodel.GmailScanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GmailScanViewModelTest {

    // El ViewModel lanza corrutinas en viewModelScope (Dispatchers.Main). En tests fijamos
    // Main a un dispatcher de test "unconfined" que las ejecuta de forma inmediata.
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun detected(id: Long) = GmailDetected(
        id = id, serviceName = "Netflix", domain = "netflix.com",
        senderEmail = "b@netflix.com", lastSeen = "2026-05-01", price = 14.99,
        currency = "EUR", billingCycle = "MONTHLY", priceFromEmail = true
    )

    @Test
    fun `onReturn carga resultados y pasa a estado Results`() = runTest {
        val vm = GmailScanViewModel(FakeRepo(results = listOf(detected(1), detected(2))))
        vm.onReturnedFromConsent("ok")
        val state = vm.uiState.value
        assertTrue(state is GmailScanUiState.Results)
        assertEquals(2, (state as GmailScanUiState.Results).items.size)
        assertEquals(setOf(1L, 2L), vm.selectedIds.value)
    }

    @Test
    fun `onReturn con status error pasa a Error`() = runTest {
        val vm = GmailScanViewModel(FakeRepo())
        vm.onReturnedFromConsent("error")
        assertTrue(vm.uiState.value is GmailScanUiState.Error)
    }

    @Test
    fun `onReturn sin resultados pasa a Empty`() = runTest {
        val vm = GmailScanViewModel(FakeRepo(results = emptyList()))
        vm.onReturnedFromConsent("ok")
        assertTrue(vm.uiState.value is GmailScanUiState.Empty)
    }

    @Test
    fun `toggle cambia la seleccion`() = runTest {
        val vm = GmailScanViewModel(FakeRepo(results = listOf(detected(1), detected(2))))
        vm.onReturnedFromConsent("ok")
        vm.toggle(1L)
        assertEquals(setOf(2L), vm.selectedIds.value)
    }

    @Test
    fun `addSelected da de alta y pasa a Done`() = runTest {
        val repo = FakeRepo(results = listOf(detected(1)), addResult = GmailAddResult(added = 1))
        val vm = GmailScanViewModel(repo)
        vm.onReturnedFromConsent("ok")
        vm.addSelected()
        val state = vm.uiState.value
        assertTrue(state is GmailScanUiState.Done)
        assertEquals(1, (state as GmailScanUiState.Done).added)
    }
}

private class FakeRepo(
    private val results: List<GmailDetected> = emptyList(),
    private val ticketUrl: String = "https://accounts.google.com/o/oauth2/v2/auth?x=1",
    private val addResult: GmailAddResult = GmailAddResult(added = 0)
) : GmailScanRepository {
    override suspend fun requestTicket(months: Int) = Result.success(GmailScanTicketResponse(ticketUrl))
    override suspend fun getResults() = Result.success(results)
    override suspend fun add(ids: List<Long>) = Result.success(addResult)
}
