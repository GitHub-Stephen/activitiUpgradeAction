package com.redxun.util;

import org.activiti.engine.*;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.List;
import java.util.Scanner;


public class CompatibilityProcessRepairTool {
    
    private static final String EXPORT_DIR = "/Users/zhuweicong/IdeaProjects/ac_boot3_7.3_2_7.4/ac-boot/Release_activiti/exported/";
    
    public static void main(String[] args) throws Exception {
    
        String action = args[0].toLowerCase();
    
        switch (action) {
            case "export":
            case "redeploy":
                goToProcess(action);
                break;
            case "help":
            case "-h":
            case "--help":
                printUsage();
                break;
            default:
                System.out.println("无效参数：" + action);
                printUsage();
                break;
        }
        
    }
    
    private static void goToProcess(String action) {
        Scanner scanner = new Scanner(System.in);
        Console console = System.console();
        
        System.out.println("=== 兼容性流程图修复工具 ===");
        
        System.out.print("请输入导出流程图的路径（如：/path/to/export/diagram.png）：");
        String exportPath = scanner.nextLine();
        
        System.out.print("请输入 JDBC URL（如：jdbc:mysql://localhost:3306/dbname）：");
        String jdbcUrl = scanner.nextLine();
        
        System.out.print("请输入数据库用户名：");
        String username = scanner.nextLine();
        
        String password;
        if (console != null) {
            char[] pwdArray = console.readPassword("请输入数据库密码：");
            password = new String(pwdArray);
        } else {
            System.out.print("请输入数据库密码（注意：当前环境不支持隐藏输入）：");
            password = scanner.nextLine();
        }
        
        try {
            System.out.println("\n开始处理...");
            // 假设原来的类中有一个叫 process 的静态方法
            process(exportPath, jdbcUrl, username, password, action);
            System.out.println("处理完成！");
        } catch (Exception e) {
            System.err.println("处理过程中发生错误：" + e.getMessage());
            e.printStackTrace();
        }
        
        scanner.close();
    }
    
    private static void printUsage() {
        System.out.println("用法: CompatibilityProcessRepairTool <操作类型>");
        System.out.println("可用操作类型:");
        System.out.println("  export    仅导出流程定义文件（bpmn）");
        System.out.println("  redeploy  仅重新部署流程定义文件");
        System.out.println("  help      显示此帮助信息");
        System.out.println("\n示例:");
        System.out.println("  ./CompatibilityProcessRepairTool export");
        System.out.println("  ./CompatibilityProcessRepairTool redeploy");
    }
    
    private static void process(String exportPath, String jdbcUrl, String username, String password,String action)
            throws IOException {
        ProcessEngineConfiguration cfg = ProcessEngineConfiguration
                .createStandaloneProcessEngineConfiguration();
    
        // ⚠️ 修改为你自己的数据库连接信息
        //cfg.setJdbcUrl("jdbc:mysql://192.168.2.29:3306/ac_boot_wc_test?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true");
        cfg.setJdbcUrl(jdbcUrl);
        cfg.setJdbcUsername(username);
        cfg.setJdbcPassword(password);
        cfg.setJdbcDriver("com.mysql.cj.jdbc.Driver");
        cfg.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE);
    
        ProcessEngine processEngine = cfg.buildProcessEngine();
        RepositoryService repositoryService = processEngine.getRepositoryService();
        
        // Step 1: 导出现有部署流程文件
        if("export".equals(action)){
            System.out.println("执行导出bpmn文件");
            //exportBpmnFiles(repositoryService,exportPath);
        } else if ("redeploy".equals(action)) {
            System.out.println("执行重新部署");
    
            // Step 2: 使用 8.7 逻辑重新部署
            //redeployBpmnFiles(repositoryService,exportPath);
        }
        // Step 3（可选）暂停新流程定义
        //suspendNewProcessDefinitions(repositoryService);
    
        processEngine.close();
        System.out.println("✅ 兼容性流程修复完成");
    }
    
    /**
     * 导出所有部署中的 .bpmn/.bpmn20.xml 文件
     */
    private static void exportBpmnFiles(RepositoryService repositoryService, String exportPath) throws IOException {
        List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
        new File(exportPath).mkdirs();
        System.out.printf("共有%s 条流程定义", deployments.size());
        for (Deployment deployment : deployments) {
            List<String> resources = repositoryService.getDeploymentResourceNames(deployment.getId());
            
            for (String resource : resources) {
                if (resource.endsWith(".bpmn") || resource.endsWith(".bpmn20.xml")) {
                    InputStream is = repositoryService.getResourceAsStream(deployment.getId(), resource);
                    File target = new File(exportPath + "/" + resource);
                    try (FileOutputStream fos = new FileOutputStream(target)) {
                        IOUtils.copy(is, fos);
                    }
                    System.out.println("📤 导出流程：" + resource);
                }
            }
        }
    }
    
    /**
     * 将 .bpmn 文件重新部署，兼容 Activiti 8.7 的结构要求
     */
    private static void redeployBpmnFiles(RepositoryService repositoryService, String exportPath) throws IOException {
        File dir = new File(exportPath + "/process");
        File[] bpmnFiles = dir.listFiles((d, name) -> name.endsWith(".bpmn") || name.endsWith(".bpmn20.xml"));
        
        if (bpmnFiles == null || bpmnFiles.length == 0) {
            System.out.println("⚠️ 未找到可重新部署的 .bpmn 文件");
            return;
        }
    
        for (File file : bpmnFiles) {
            String fileName = file.getName();
    
            String processKey = fileName.replace(".bpmn20.xml", "").replace(".bpmn", "");
    
            DeploymentBuilder builder = repositoryService.createDeployment()
                    .name(processKey);
            // 切割掉后缀，提取 key
            try (InputStream is = new FileInputStream(file)) {
                builder.addInputStream(file.getName(), is);
                builder.deploy();
                System.out.println("✅ 已重新部署流程：" + file.getName());
            }
           
        }
        System.out.printf("成功部署 %s 条流程定义", bpmnFiles.length);
    }
    
    /**
     * 暂停最新部署的流程定义，避免干扰旧实例（可选）
     */
    private static void suspendNewProcessDefinitions(RepositoryService repositoryService) {
        List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
                .orderByProcessDefinitionVersion().desc()
                .list();
        
        for (ProcessDefinition pd : definitions) {
            if (!pd.isSuspended()) {
                repositoryService.suspendProcessDefinitionById(pd.getId());
                System.out.println("⛔ 已暂停流程定义：" + pd.getKey() + " (version: " + pd.getVersion() + ")");
            }
        }
    }
}
