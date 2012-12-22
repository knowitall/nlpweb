SELECT * FROM LogEntry entry
JOIN Param p on p.LOGENTRY_ID = entry.LOGENTRY_ID
WHERE entry.LOGENTRY_ID = :id
