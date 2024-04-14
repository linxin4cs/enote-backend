package sit.zlx.enotebackend.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import sit.zlx.enotebackend.controller.AdminController.UsageBody;
import sit.zlx.enotebackend.controller.AdminController.BarChartDataBody;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

public interface AdminService {
    UsageBody.Size getUsageSize(UsageBody.Size totalSize, List<Long> sizes, String parsedTotalSize);
    void formatAndSortBarChartData(BarChartDataBody barChartDataBody, SimpleDateFormat sdf, List<BarChartDataBody.BarChartDataItem> data);
    @Async
    @Transactional
    void deleteUserFiles(List<String> ids) throws IOException;
    @Async
    @Transactional
    void deleteUserRelatedRecords(List<String> ids);
}
