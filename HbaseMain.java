package myspark.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hadoop on 18-2-23.
 */
public class HbaseMain {
    static Configuration config = null;
    private Connection connection = null;


    public static void main(String[] args) throws IOException {
        HbaseMain hmain = new HbaseMain();
        hmain.init();
        String[] families = {"info1", "info2"};
        String tableName = "test";
        hmain.createTable(tableName, families);

        String[] newFamilies = {"info3", "info4"};
        hmain.editTable(tableName, newFamilies, "add");

        String[] delFamilies = {"info4", "info5"};
        hmain.editTable(tableName, delFamilies, "del");


        // hmain.dropTable(tableName);

        //hmain.singlePut();

        //hmain.batchPut();

        //hmain.deleteData();
        //hmain.queryData();

        // hmain.scanData();

        hmain.filter();
    }

    /**
     * 初始化连接
     *
     * @throws IOException
     */
    public void init() throws IOException {
        config = HBaseConfiguration.create();
        config.set("hbase.zookeeper.quorum", "hadoop001");
        config.set("hbase.zookeeper.property.clientPort", "2181");
        connection = ConnectionFactory.createConnection(config);
    }


    /**
     * 创建表
     *
     * @param tablename
     * @param families  列族数组
     */
    public void createTable(String tablename, String[] families) {

        try {
            // hbase 表管理器
            Admin admin = connection.getAdmin();
            // 操作的表名
            TableName tableName = TableName.valueOf(tablename);
            if (!admin.tableExists(tableName)) {
                // hbase表模式
                HTableDescriptor tableDescriptor = new HTableDescriptor(tableName);

                // 添加列族
                for (String family : families) {
                    tableDescriptor.addFamily(new HColumnDescriptor(family.getBytes()));
                }

                // 创建表
                admin.createTable(tableDescriptor);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 删除表
     *
     * @param tablename
     */
    public void dropTable(String tablename) {
        try {
            // hbase 表管理器
            Admin admin = connection.getAdmin();
            // 操作的表名
            TableName tableName = TableName.valueOf(tablename);
            admin.disableTable(tableName);
            admin.deleteTable(tableName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 增加或者删除列
     *
     * @param tablename
     * @param families
     * @param action    对表列的操作类型， del-删除， add-添加
     */
    public void editTable(String tablename, String[] families, String action) {
        try {

            if (!"add".equals(action) && !"del".equals(action)) {
                System.err.println("action err. expected:add|del");
            }
            // hbase 表管理器
            Admin admin = connection.getAdmin();
            // 操作的表名
            TableName tableName = TableName.valueOf(tablename);

            if (admin.tableExists(tableName)) {
                // hbase表模式
                HTableDescriptor tableDescriptor = admin.getTableDescriptor(tableName);
                HColumnDescriptor[] columnDescriptors = tableDescriptor.getColumnFamilies();

                // 添加列族
                for (String family : families) {
                    if ("add".equals(action)) {
                        if (!tableDescriptor.hasFamily(family.getBytes())) {
                            tableDescriptor.addFamily(new HColumnDescriptor(family.getBytes()));
                            admin.modifyTable(tableName, tableDescriptor);
                        }
                    } else if ("del".equals(action)) {
                        if (tableDescriptor.hasFamily(family.getBytes())) {
                            admin.deleteColumn(tableName, family.getBytes());
                        }
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 单条插入数据
     *
     * @throws IOException
     */
    public void singlePut() throws IOException {
        String tablename = "test";

        Table table = connection.getTable(TableName.valueOf(tablename));

        try {

            BufferedMutator bufferedMutator = connection.getBufferedMutator(TableName.valueOf(tablename));
            long begin = System.currentTimeMillis();

            for (int i = 0; i < 10000; i++) {
                String rowkey = String.format("%05d", i);
                Put put = new Put(rowkey.getBytes());
                put.addColumn(Bytes.toBytes("info1"), Bytes.toBytes("good"), Bytes.toBytes("goood" + Math.random()));
                put.addColumn(Bytes.toBytes("info1"), Bytes.toBytes("price"), Bytes.toBytes(Math.random()));
                bufferedMutator.mutate(put);
                //table.put(put);
            }

            long end = System.currentTimeMillis();
            bufferedMutator.flush();

            System.out.println((end - begin) + "ms");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 批量插入数据
     *
     * @throws IOException
     */
    public void batchPut() throws IOException {

        String tablename = "test";

        Table table = connection.getTable(TableName.valueOf(tablename));

        long begin = System.currentTimeMillis();
        List<Put> puts = new ArrayList<Put>();
        for (int i = 0; i < 100000; i++) {
            String rowkey = String.format("%05d", i);
            Put put = new Put(rowkey.getBytes());
            put.addColumn(Bytes.toBytes("info1"), Bytes.toBytes("good"), Bytes.toBytes("goood" + Math.random()));
            puts.add(put);
        }

        table.put(puts);

        long end = System.currentTimeMillis();
        System.out.println((end - begin) + "ms");
    }

    /**
     * 删除数据
     *
     * @throws IOException
     */

    public void deleteData() throws IOException {
        String tablename = "test";

        Table table = connection.getTable(TableName.valueOf(tablename));

        long begin = System.currentTimeMillis();
        List<Delete> deletes = new ArrayList<Delete>();
        for (int i = 0; i < 100000; i++) {
            String rowkey = String.format("%05d", i);
            Delete delete = new Delete(rowkey.getBytes());
            //table.delete(delete);
            deletes.add(delete);
        }

        table.delete(deletes);
        long end = System.currentTimeMillis();
        System.out.println((end - begin) + "ms");
    }


    /**
     * 查询单条记录
     * @throws IOException
     */
    public void queryData() throws IOException {
        String tablename = "test";
        Table table = connection.getTable(TableName.valueOf(tablename));

        Get get = new Get(Bytes.toBytes("00001"));
        Result result = table.get(get);

        Map<String, String> resultMap = getMapData(result);
        for (Map.Entry<String, String> entry : resultMap.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }

    }

    public void scanData() throws IOException {
        String tablename = "test";
        Table table = connection.getTable(TableName.valueOf(tablename));

        Scan scan = new Scan();
        scan.setStartRow("00001".getBytes());
        scan.setStopRow("00010".getBytes());

        ResultScanner results = table.getScanner(scan);

        for(Result r: results){
            Map<String, String> resultMap = getMapData(r);
            System.out.println(resultMap);
        }

    }

    /**
     * 将查询的记录集进行转换
     * @param result
     * @return
     */
    public Map<String, String> getMapData(Result result) {
        List<Cell> cells = result.listCells();
        String rowName = "";
        Map<String, String> cellMap = new HashMap<String, String>();

        for (Cell cell : cells) {

            // 获取rowkey
            short rowLen = cell.getRowLength();
            byte[] rowBytes = new byte[rowLen];
            System.arraycopy(cell.getRowArray(), cell.getRowOffset(), rowBytes, 0, rowLen);

            // 获取列族名
            short fLen = cell.getFamilyLength();
            byte[] fBytes = new byte[fLen];
            System.arraycopy(cell.getFamilyArray(), cell.getFamilyOffset(), fBytes, 0, fLen);

            // 获取列名
            int qLen = cell.getQualifierLength();
            byte[] qBytes = new byte[qLen];
            System.arraycopy(cell.getQualifierArray(), cell.getQualifierOffset(), qBytes, 0, qLen);

            // 获取列值
            int cLen = cell.getValueLength();
            byte[] cBytes = new byte[cLen];
            System.arraycopy(cell.getValueArray(), cell.getValueOffset(), cBytes, 0, cLen);

            rowName = Bytes.toString(rowBytes);
            String fName = Bytes.toString(fBytes);
            String qName = Bytes.toString(qBytes);
            String cValue = Bytes.toString(cBytes);

            cellMap.put(fName + ":" + qName, cValue);
        }

        cellMap.put("rowkey", rowName);

        return cellMap;
    }


    /**
     * 过滤器
     * @throws IOException
     */
    public void filter() throws IOException {
        String tablename = "test";
        Table table = connection.getTable(TableName.valueOf(tablename));

        // MUST_PASS_ONE 各个Filter之间是或的关系
        // MUST_PASS_ALL 各个Filter之间是与的关系
        FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);


        // 过滤器一： SingleColumnValueFilter 过滤列相等/大于/小于等
        SingleColumnValueFilter singleColumnValueFilter = new SingleColumnValueFilter(Bytes.toBytes("info1"), Bytes.toBytes("good"), CompareFilter.CompareOp.GREATER_OR_EQUAL, Bytes.toBytes("goood0.43621999969130765"));
        filterList.addFilter(singleColumnValueFilter);
        SingleColumnValueFilter singleColumnValueFilter1 = new SingleColumnValueFilter(Bytes.toBytes("info1"), Bytes.toBytes("good"), CompareFilter.CompareOp.GREATER_OR_EQUAL, Bytes.toBytes("goood0.25972"));
        //filterList.addFilter(singleColumnValueFilter1);

        // 过滤器二： 过滤列名的前缀
        ColumnPrefixFilter columnPrefixFilter = new ColumnPrefixFilter( Bytes.toBytes("goo"));
        //filterList.addFilter(columnPrefixFilter);

        // 过滤器三： 多个列名
        byte[][] multiColumns = new byte[][]{Bytes.toBytes("goo"), Bytes.toBytes("te")};
        MultipleColumnPrefixFilter multipleColumnPrefixFilter = new MultipleColumnPrefixFilter(multiColumns);
        //filterList.addFilter(multipleColumnPrefixFilter);

        // 过滤器四： rowkey
        RowFilter rowFilter = new RowFilter(CompareFilter.CompareOp.EQUAL, new RegexStringComparator("^0002"));
        //filterList.addFilter(rowFilter);

        Scan scan = new Scan();
        scan.setStartRow("00001".getBytes());
        scan.setStopRow("00050".getBytes());
        scan.setFilter(filterList);

        ResultScanner results = table.getScanner(scan);

        for(Result r: results){
            Map<String, String> resultMap = getMapData(r);
            System.out.println(resultMap);
        }
    }
}

