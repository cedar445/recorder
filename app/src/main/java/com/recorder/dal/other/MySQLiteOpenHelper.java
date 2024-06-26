package com.recorder.dal.other;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.format.DateUtils;

import androidx.annotation.Nullable;

import com.recorder.dal.bean.Bill;
import com.recorder.dal.bean.Category;
import com.recorder.dal.bean.Merchant;
import com.recorder.dal.bean.Statistic;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MySQLiteOpenHelper extends SQLiteOpenHelper {

    //数据库名称
    private static final String DB_NAME = "Account.db";
    //数据库表格名称
    private static final String TABLE_NAME_BILL = "bill";
    private static final String TABLE_NAME_CATEGORY = "category";
    private static final String TABLE_NAME_MERCHANT = "merchant";

    //数据库建表操作
    private static final String CREATE_TABLE_BILL = "create table "+TABLE_NAME_BILL+"(billID INT PRIMARY KEY AUTOINCREMENT,\n" +
            "type VARCHAR(50) NOT NULL,\n" +
            "dateTime DATETIME NOT NULL,\n" +
            "money FLOAT NOT NULL,\n" +
            "cateID INT NOT NULL,\n" +
            //"category VARCHAR(255),\n "+
            "remarks TEXT,\n" +
            "FOREIGN KEY (cateID) REFERENCES Category(cateID))";
    private static final String CREATE_TABLE_CATEGORY = "create table "+TABLE_NAME_CATEGORY+"(cateID INT PRIMARY KEY,\n" +
            "name VARCHAR(255) NOT NULL,\n" +
            "description TEXT)";
    private static final String CREATE_TABLE_MERCHANT = "create table "+TABLE_NAME_MERCHANT+"(name VARCHAR(255) NOT NULL,\n" +
            "cateID INT,\n" +
            "FOREIGN KEY (cateID) REFERENCES Category(cateID))";

    //查询语句，计算收入支出总和
    private static final String QUERY_TOTAL_INCOME = "SELECT SUM(money) as totalIncome FROM "+TABLE_NAME_BILL+" WHERE type = 'Income' AND dateTime BETWEEN ? AND ?";
    private static final String QUERY_TOTAL_EXPENSE = "SELECT SUM(money) as totalExpense FROM "+TABLE_NAME_BILL+" WHERE type = 'Expense' AND dateTime BETWEEN ? AND ?";


    public MySQLiteOpenHelper(Context context) {
        super(context, DB_NAME, null, 1);
    }


    //创建数据库
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_BILL);
        db.execSQL(CREATE_TABLE_CATEGORY);
        db.execSQL(CREATE_TABLE_MERCHANT);
        init();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void init(){
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("cateID","1");
        values.put("name","try");
        values.put("description","JUST A TRY");

        db.insert(TABLE_NAME_CATEGORY,null,values);
        ContentValues _values = new ContentValues();
        _values.put("type","expense");
        _values.put("dateTime","2024-5-30");
        _values.put("money","100");
        _values.put("cateID","1");
        _values.put("remarks","Just a try");
        db.insert(TABLE_NAME_BILL,null,_values);
    }

    //增删改查

    /*
    * insert方法返回值为-1时，代表插入错误
    * 否则，返回row ID
    * */
    public long insertBill(Bill bill){
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("type",bill.getType());
        values.put("dateTime",bill.getDateTime());
        values.put("money",bill.getMoney());
        values.put("cateID",bill.getCateID());
        values.put("remarks",bill.getRemarks());

        return db.insert(TABLE_NAME_BILL,null,values);
    }
    public long insertCategory(Category category){
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("cateID",category.getCateID());
        values.put("name",category.getName());
        values.put("description",category.getDescription());

        return db.insert(TABLE_NAME_CATEGORY,null,values);
    }
    public long insertMerchant(Merchant merchant){
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name",merchant.getName());
        values.put("cateID",merchant.getCateID());

        return db.insert(TABLE_NAME_MERCHANT,null,values);
    }

    /*
    *delete方法返回值为删除的记录数
    *Bill按照ID删除、Category按照name删除、Merchant按照name删除
    * */
    public long deleteFromBillByID(int id){
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_NAME_BILL,"billID = ?",new String[]{String.valueOf(id)});
    }
    public long deleteFromCategoryByName(String name){
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_NAME_CATEGORY,"name = ?",new String[]{name});
    }
    public long deleteFromMerchantByName(String name){
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_NAME_MERCHANT,"name = ?",new String[]{name});
    }

    /*
    * update方法返回值和delete一样
    * 修改条件Category按照cateID查询修改，其余与delete条件相同
    * 需要参数和insert一样
    * */
    public long updateBill(Bill bill){
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("type",bill.getType());
        values.put("dateTime",bill.getDateTime());
        values.put("money",bill.getMoney());
        values.put("cateID",bill.getCateID());
        values.put("remarks",bill.getRemarks());

        return db.update(TABLE_NAME_BILL,values,"billID = ?",new String[]{String.valueOf(bill.getBillID())});
    }
    public long updateCategory(Category category){
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("cateID",category.getCateID());
        values.put("name",category.getName());
        values.put("description",category.getDescription());

        return db.update(TABLE_NAME_CATEGORY,values,"cateID = ?",new String[]{String.valueOf(category.getCateID())});
    }
    public long updateMerchant(Merchant merchant){
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name",merchant.getName());
        values.put("cateID",merchant.getCateID());

        return db.update(TABLE_NAME_MERCHANT,values,"name = ?",new String[]{merchant.getName()});
    }

    /*
    *query实现通过billID、cateID、type、dateTime以及大于小于等于money的查询功能
    * */
    public List<Bill> queryFromBillByOther(int billID,int cateID,String type,float money,String choice) throws ParseException {

        SQLiteDatabase db = getWritableDatabase();
        List<Bill> billList = new ArrayList<>();
        Cursor cursor;
        if(choice.equals("大于等于")){
            cursor = db.query(TABLE_NAME_BILL,null,"billID = ? and cateID = ? and type like ? and money >= ?",
                    new String[]{String.valueOf(billID),String.valueOf(cateID),type,String.valueOf(money)},
                    null,null,null);
        }else if(choice.equals("小于等于")){
            cursor = db.query(TABLE_NAME_BILL,null,"billID = ? and cateID = ? and type like ? and money <= ?",
                    new String[]{String.valueOf(billID),String.valueOf(cateID),type,String.valueOf(money)},
                    null,null,null);
        }else{
            cursor = db.query(TABLE_NAME_BILL,null,"billID = ? and cateID = ? and type like ? ",
                    new String[]{String.valueOf(billID),String.valueOf(cateID),type},
                    null,null,null);
        }
        //Cursor cursor_cate = db.query(TABLE_NAME_CATEGORY,)

        if(cursor != null){
            while(cursor.moveToNext()){
                int _billID = cursor.getInt(cursor.getColumnIndexOrThrow("billID"));
                int _cateID = cursor.getInt(cursor.getColumnIndexOrThrow("cateID"));
                String _type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                String date= cursor.getString(cursor.getColumnIndexOrThrow("dateTime"));
                SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd");
                Date dateTime = ft.parse(date);
                float _money = cursor.getFloat(cursor.getColumnIndexOrThrow("money"));
                String remarks = cursor.getString(cursor.getColumnIndexOrThrow("remarks"));

                Bill bill = new Bill();
                bill.setBillID(_billID);
                bill.setCateID(_cateID);
                bill.setType(_type);
                bill.setDateTime(dateTime);
                bill.setMoney(_money);
                bill.setRemarks(remarks);

                billList.add(bill);
            }
            cursor.close();
            db.close();
        }
        return billList;
    }
    public List<Bill> queryFromBillByDate(Date startDate,Date endDate) throws ParseException {

        SQLiteDatabase db = getWritableDatabase();
        List<Bill> billList = new ArrayList<>();

        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd");
        String startDateStr = ft.format(startDate);
        String endDateStr = ft.format(endDate);

        String selection = "dateTime >= ? and dateTime <= ?";
        String[] selectionArgs = new String[]{startDateStr,endDateStr};
        Cursor cursor = db.query(TABLE_NAME_BILL,null,selection,selectionArgs,
                null,null,null);

        if(cursor != null){
            while(cursor.moveToNext()){
                int _billID = cursor.getInt(cursor.getColumnIndexOrThrow("billID"));
                int _cateID = cursor.getInt(cursor.getColumnIndexOrThrow("cateID"));
                String _type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                String date= cursor.getString(cursor.getColumnIndexOrThrow("dateTime"));
                //SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd");
                Date dateTime = ft.parse(date);
                float _money = cursor.getFloat(cursor.getColumnIndexOrThrow("money"));
                String remarks = cursor.getString(cursor.getColumnIndexOrThrow("remarks"));

                Bill bill = new Bill();
                bill.setBillID(_billID);
                bill.setCateID(_cateID);
                bill.setType(_type);
                bill.setDateTime(dateTime);
                bill.setMoney(_money);
                bill.setRemarks(remarks);

                billList.add(bill);

            }
            cursor.close();
            db.close();
        }
        return billList;
    }

    //查找指定名称的cateID列，以及备注
    public List<Category> queryFromCategoryByName(String name){

        SQLiteDatabase db = getWritableDatabase();
        List<Category> categoryList = new ArrayList<>();
        Cursor cursor = db.query(TABLE_NAME_CATEGORY,new String[]{"cateID","description"},"name like ?",new String[]{name},
                null,null,null);

        if(cursor != null){
            while(cursor.moveToNext()){
                //String _name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                int _cateID = cursor.getInt(cursor.getColumnIndexOrThrow("cateID"));
                String _description = cursor.getString(cursor.getColumnIndexOrThrow("description"));

                Category category = new Category();
                category.setCateID(_cateID);
                category.setDescription(_description);
                //category.setName(name);
                categoryList.add(category);
            }
            cursor.close();
            db.close();
        }
        return categoryList;
    }
    //只查询cateID的方法，方便下面检索统计数据的实现！！！
    public List<Integer> queryFromCategoryByNameToID(String name){

        SQLiteDatabase db = getWritableDatabase();
        List<Integer> cateIDList = null;
        Cursor cursor = db.query(TABLE_NAME_CATEGORY,new String[]{"cateID"},"name like ?",new String[]{name},
                null,null,null);

        if(cursor != null){
            while(cursor.moveToNext()){
                //String _name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                int _cateID = cursor.getInt(cursor.getColumnIndexOrThrow("cateID"));
                cateIDList.add(_cateID);
            }
            cursor.close();
            db.close();
        }
        return cateIDList;
    }


    /*检索统计数据
    * 实现了给出起始日期的查询方法
    * */
    public Statistic getStatisticByDate(Date startDate,Date endDate){
        SQLiteDatabase db = getWritableDatabase();
        Statistic statistic = null;

        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat ft_time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String startDateStr = ft.format(startDate);
        String endDateStr = ft.format(endDate);
        Date now = new Date();
        String nowTime = ft_time.format(now);

        Cursor cursorIncome=db.rawQuery(QUERY_TOTAL_INCOME,new String[]{startDateStr,endDateStr});
        float totalIncome = 0;
        if(cursorIncome.moveToFirst()){
            totalIncome = cursorIncome.getColumnIndexOrThrow("totalIncome");
        }
        cursorIncome.close();
        Cursor cursorExpense=db.rawQuery(QUERY_TOTAL_EXPENSE,new String[]{startDateStr,endDateStr});
        float totalExpense = 0;
        if(cursorExpense.moveToFirst()){
            totalExpense = cursorExpense.getColumnIndexOrThrow("totalExpense");
        }
        cursorExpense.close();
        db.close();

        statistic = new Statistic(nowTime, totalIncome,totalExpense,startDate, endDate);
        return statistic;
    }

    /*写的有点抽象，有时间改
    * 实现了按照分类名称检索统计数据
    * */
    public Statistic getStatisticByCategory(String name){
        List<Integer> cateIDList = queryFromCategoryByNameToID(name);
        SQLiteDatabase db = getWritableDatabase();
        Statistic statistic = null;

        SimpleDateFormat ft_time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        String nowTime = ft_time.format(now);

        String inClause = cateIDList.toString().replace("[","(").replace("]",")");
        String QUERY_TOTAL_INCOME_CATE = "SELECT SUM(money) as totalIncome FROM "+TABLE_NAME_BILL+" cateID IN "+inClause;
        String QUERY_TOTAL_EXPENSE_CATE = "SELECT SUM(money) as totalExpense FROM "+TABLE_NAME_BILL+" cateID IN "+inClause;

        Cursor cursorIncome=db.rawQuery(QUERY_TOTAL_INCOME_CATE,null);
        float totalIncome = 0;
        if(cursorIncome.moveToFirst()){
            totalIncome = cursorIncome.getColumnIndexOrThrow("totalIncome");
        }
        cursorIncome.close();
        Cursor cursorExpense=db.rawQuery(QUERY_TOTAL_EXPENSE_CATE,null);
        float totalExpense = 0;
        if(cursorExpense.moveToFirst()){
            totalExpense = cursorExpense.getColumnIndexOrThrow("totalExpense");
        }
        cursorExpense.close();
        db.close();

        statistic = new Statistic();
        statistic.setStatisticID(nowTime);
        statistic.setName(name);
        statistic.setTotalIncome(totalIncome);
        statistic.setTotalExpense(totalExpense);
        return statistic;
    }

}
