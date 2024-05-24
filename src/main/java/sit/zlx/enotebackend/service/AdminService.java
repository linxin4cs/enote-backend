package sit.zlx.enotebackend.service;

import sit.zlx.enotebackend.controller.AdminController.BarChartDataBody;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface AdminService {
    void formatAndSortBarChartData(BarChartDataBody barChartDataBody, SimpleDateFormat sdf, List<BarChartDataBody.BarChartDataItem> data);
    CompletableFuture<Void> deleteUserFiles(List<String> ids) throws IOException;
    CompletableFuture<Void> deleteUserRelatedRecords(List<String> ids);
    void deleteUserData(List<String> ids) throws IOException, ExecutionException, InterruptedException;
}
