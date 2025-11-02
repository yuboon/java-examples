package com.example.report.controller;

import com.example.report.entity.ColumnInfo;
import com.example.report.entity.QueryRequest;
import com.example.report.entity.TableInfo;
import com.example.report.service.MetaDataService;
import com.example.report.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/report")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private MetaDataService metaDataService;

    @Autowired
    private QueryService queryService;

    @GetMapping("/databases")
    public ResponseEntity<List<String>> getDatabases() {
        List<String> databases = metaDataService.getDatabases();
        return ResponseEntity.ok(databases);
    }

    @GetMapping("/tables/{database}")
    public ResponseEntity<List<TableInfo>> getTables(@PathVariable String database) {
        List<TableInfo> tables = metaDataService.getDatabaseTables(database);
        return ResponseEntity.ok(tables);
    }

    @GetMapping("/columns/{database}/{table}")
    public ResponseEntity<List<ColumnInfo>> getColumns(
            @PathVariable String database,
            @PathVariable String table) {
        List<ColumnInfo> columns = metaDataService.getTableColumns(database, table);
        return ResponseEntity.ok(columns);
    }

    @PostMapping("/query/{database}")
    public ResponseEntity<List<Map<String, Object>>> executeQuery(
            @PathVariable String database,
            @RequestBody QueryRequest request) {
        List<Map<String, Object>> result = queryService.executeQuery(request, database);
        return ResponseEntity.ok(result);
    }
}