package com.doomsday.game.tool.repo;

import com.doomsday.game.tool.model.ToolCallAudit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ToolCallAuditRepository extends JpaRepository<ToolCallAudit, Long> {

    @Query(value = """
            SELECT tool_name,
                   COUNT(1) AS total_calls,
                   SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_calls,
                   SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed_calls,
                   COALESCE(AVG(latency_ms), 0) AS avg_ms,
                   COALESCE(AVG(retry_count), 0) AS avg_retry
            FROM tool_call_audit
            GROUP BY tool_name
            ORDER BY tool_name
            """, nativeQuery = true)
    List<Object[]> aggregateSummary();

    List<ToolCallAudit> findTop50ByOrderByCreatedAtDesc();
}
