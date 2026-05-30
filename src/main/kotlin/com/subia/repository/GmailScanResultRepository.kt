package com.subia.repository

import com.subia.model.GmailScanResultRow
import org.springframework.data.jpa.repository.JpaRepository

interface GmailScanResultRepository : JpaRepository<GmailScanResultRow, Long> {
    fun findByUserId(userId: Long): List<GmailScanResultRow>
    fun findByIdInAndUserId(ids: List<Long>, userId: Long): List<GmailScanResultRow>
    fun deleteByUserId(userId: Long)
    fun deleteByIdInAndUserId(ids: List<Long>, userId: Long)
}
