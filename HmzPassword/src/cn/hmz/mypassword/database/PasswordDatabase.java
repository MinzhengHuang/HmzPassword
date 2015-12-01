package cn.hmz.mypassword.database;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import cn.hmz.mypassword.R;
import cn.hmz.mypassword.app.AES;
import cn.hmz.mypassword.app.MyApplication;
import cn.hmz.mypassword.model.Password;
import cn.hmz.mypassword.model.PasswordGroup;
import cn.hmz.mypassword.model.SettingKey;

/**
 * 保存所有的密码数据
 *
 */
public class PasswordDatabase extends SQLiteOpenHelper {
	private static final int version = 4;
	private Context context;

	/** AES加密密匙 */
	private String currentPasswd;

	public PasswordDatabase(Context context) {
		super(context, "password", null, version);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		createPasswordTable(db);

		createGroupTable(db);
	}

	private void createPasswordTable(SQLiteDatabase db) {
		String sql = "create table password(id integer primary key autoincrement, create_date integer, title text, "
				+ "user_name text, password text, is_top integer default 0, note text, group_name text default '"
				+ getDefaultGroupName() + "')";
		db.execSQL(sql);
	}

	private String getDefaultGroupName() {
		return context.getString(R.string.password_group_default_name);
	}

	private void createGroupTable(SQLiteDatabase db) {
		String sql;
		sql = "create table password_group(name text primary key)";
		db.execSQL(sql);

		sql = "insert into password_group(name) values('" + getDefaultGroupName() + "')";
		db.execSQL(sql);
		getMyApplication().putSetting(SettingKey.LAST_SHOW_PASSWORDGROUP_NAME, getDefaultGroupName());
	}

	private MyApplication getMyApplication() {
		return (MyApplication) context.getApplicationContext();
	}

	/**
	 * <p>
	 * 1 ---> 初始版本，含有一个表password(id, create_date, title, user_name,
	 * password，note)
	 * <p>
	 * 2 ---> password表添加is_top字段，默认值为零，表示不置顶
	 * <p>
	 * 3 --->password表添加group_name字段，表示分组，默认值：默认；增加group(name)表，增加分组功能
	 * <p>
	 * 4 --->对密码进行加密
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 2) {
			String sql = "alter table password add is_top integer default 0";
			db.execSQL(sql);
		}

		if (oldVersion < 3) {
			String sql = "alter table password add group_name text default '" + getDefaultGroupName() + "'";
			db.execSQL(sql);

			createGroupTable(db);
		}

		if (oldVersion < 4) {
			upgradeFor4(db);
		}
	}

	// 第四次升级， 加密数据库的所有密码
	private void upgradeFor4(SQLiteDatabase db) {
		// 取出数据库中的所有密码
		List<Password> passwords = new ArrayList<Password>();
		Cursor cursor = null;
		try {
			cursor = db.rawQuery("select id, password from password", null);
			while (cursor.moveToNext()) {
				Password password = new Password();
				password.setId(cursor.getInt(cursor.getColumnIndex("id")));
				password.setPassword(cursor.getString(cursor.getColumnIndex("password")));
				passwords.add(password);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cursor != null)
				cursor.close();
		}
		// 对密码加密后，保存数据
		for (Password password : passwords) {
			ContentValues contentValues = new ContentValues();
			contentValues.put("password", encrypt(password.getPassword()));
			db.update("password", contentValues, "id = ?", new String[] { password.getId() + "" });
		}
	}

	/**
	 * 插入一条数据
	 * 
	 * @param password
	 *            要插入的值
	 * @return 返回这条数据的自增主键 如果插入失败，返回-1
	 */
	public long insertPassword(Password password) {
		long id = -1;
		try {
			SQLiteDatabase sqLiteDatabase = getWritableDatabase();
			ContentValues contentValues = new ContentValues();
			contentValues.put("create_date", password.getCreateDate());
			contentValues.put("title", password.getTitle());
			contentValues.put("user_name", password.getUserName());
			contentValues.put("password", encrypt(password.getPassword()));
			contentValues.put("note", password.getNote());
			contentValues.put("is_top", password.isTop() ? 1 : 0);
			contentValues.put("group_name", password.getGroupName());
			id = sqLiteDatabase.insert("password", null, contentValues);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return id;
	}

	/**
	 * 更新数据
	 * 
	 * <pre>
	 * // 更新密码
	 * <code>
	 * Password password = new Password();
	 * password.setId(123);
	 * password.setPassword(&quot;新的密码&quot;);
	 * passwordDatabase.updatePassword(password);
	 * </code>
	 * @param password
	 *            更新的数据，只需要设置相应的更新项,必须有id属性
	 * @return 影响的行数 the number of rows affected
	 */
	public int updatePassword(Password password) {
		int result = 0;
		SQLiteDatabase sqLiteDatabase = getWritableDatabase();
		try {
			ContentValues contentValues = new ContentValues();
			if (password.getCreateDate() != 0)
				contentValues.put("create_date", password.getCreateDate());
			if (password.getTitle() != null)
				contentValues.put("title", password.getTitle());
			if (password.getUserName() != null)
				contentValues.put("user_name", password.getUserName());
			if (password.getPassword() != null)
				contentValues.put("password", encrypt(password.getPassword()));
			if (password.getNote() != null)
				contentValues.put("note", password.getNote());
			contentValues.put("is_top", password.isTop() ? 1 : 0);

			if (password.getGroupName() != null)
				contentValues.put("group_name", password.getGroupName());

			result = sqLiteDatabase.update("password", contentValues, "id = ?",
					new String[] { String.valueOf(password.getId()) });
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * 根据id查询数据库中的密码信息
	 * 
	 * @param id
	 * @return 查询到密码信息，如果没有该数据，返回null
	 */
	public Password getPassword(int id) {
		Password password = null;

		SQLiteDatabase sqLiteDatabase = getWritableDatabase();
		Cursor cursor = null;
		try {
			cursor = sqLiteDatabase.query("password", null, "id = ?", new String[] { String.valueOf(id) }, null, null,
					null);

			if (cursor.moveToNext()) {
				password = mapPassword(cursor);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return password;
	}

	private Password mapPassword(Cursor cursor) {
		Password password = new Password();
		password.setId(cursor.getInt(cursor.getColumnIndex("id")));
		password.setCreateDate(cursor.getLong(cursor.getColumnIndex("create_date")));
		password.setTitle(cursor.getString(cursor.getColumnIndex("title")));
		password.setUserName(cursor.getString(cursor.getColumnIndex("user_name")));
		password.setPassword(decrypt(cursor.getString(cursor.getColumnIndex("password"))));
		password.setNote(cursor.getString(cursor.getColumnIndex("note")));
		password.setTop(cursor.getInt(cursor.getColumnIndex("is_top")) == 1 ? true : false);
		password.setGroupName(cursor.getString(cursor.getColumnIndex("group_name")));
		return password;
	}

	/**
	 * 获得数据库中保存的所有密码信息
	 * 
	 * @return 返回数据，为一个数组
	 */
	public List<Password> getAllPassword() {
		List<Password> passwords = new ArrayList<Password>();
		SQLiteDatabase sqLiteDatabase = getWritableDatabase();

		Cursor cursor = null;

		try {
			cursor = sqLiteDatabase.query("password", null, null, null, null, null, null);

			while (cursor.moveToNext()) {
				Password password = null;
				password = mapPassword(cursor);

				passwords.add(password);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return passwords;
	}

	/**
	 * 删除一条数据
	 * 
	 * @param id
	 *            删除的id
	 * @return the number of rows affected if a whereClause is passed in, 0
	 *         otherwise. To remove all rows and get a count pass "1" as the
	 *         whereClause.
	 */
	public int deletePasssword(int id) {
		int result = -1;
		SQLiteDatabase sqLiteDatabase = getWritableDatabase();
		result = sqLiteDatabase.delete("password", "id = ?", new String[] { String.valueOf(id) });
		return result;
	}

	/**
	 * 根据groupName获得数据库中保存的该分组下所有的密码
	 * 
	 * @param groupName
	 *            分组名
	 * @return 返回数据，为一个数组
	 */
	public List<Password> getAllPasswordByGroupName(String groupName) {
		List<Password> passwords = new ArrayList<Password>();
		SQLiteDatabase sqLiteDatabase = getWritableDatabase();

		Cursor cursor = null;

		try {
			cursor = sqLiteDatabase.query("password", null, "group_name = ?", new String[] { groupName }, null, null,
					null);

			while (cursor.moveToNext()) {
				Password password = null;
				password = mapPassword(cursor);

				passwords.add(password);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return passwords;
	}

	/**
	 * 数据库中添加密码分组
	 * 
	 * @param passwordGroup
	 */
	public void addPasswordGroup(PasswordGroup passwordGroup) {
		try {
			SQLiteDatabase sqLiteDatabase = getWritableDatabase();
			ContentValues contentValues = new ContentValues();
			contentValues.put("name", passwordGroup.getGroupName());
			sqLiteDatabase.insert("password_group", null, contentValues);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取数据库中的所有分组
	 * 
	 * @return
	 */
	public List<PasswordGroup> getAllPasswordGroup() {
		List<PasswordGroup> passwordGroups = new ArrayList<PasswordGroup>();
		SQLiteDatabase sqLiteDatabase = getWritableDatabase();
		Cursor cursor = null;
		try {
			cursor = sqLiteDatabase.query("password_group", null, null, null, null, null, null);

			while (cursor.moveToNext()) {
				PasswordGroup passwordGroup = new PasswordGroup();
				passwordGroup.setGroupName(cursor.getString(cursor.getColumnIndex("name")));
				passwordGroups.add(passwordGroup);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cursor != null)
				cursor.close();
		}

		return passwordGroups;
	}

	/**
	 * 更新组名字
	 * 
	 * @param oldGroupName
	 *            旧名字
	 * @param newGroupName
	 *            新的名字
	 */
	public void updatePasswdGroupName(String oldGroupName, String newGroupName) {
		SQLiteDatabase sqLiteDatabase = getWritableDatabase();
		Cursor rawQuery = null;
		try {
			rawQuery = sqLiteDatabase.rawQuery("select count(name) from password_group where name = ?",
					new String[] { newGroupName });
			if (rawQuery != null && rawQuery.moveToNext() && rawQuery.getInt(0) == 1) {
				// 新的分组已经存在 直接删除旧的分组
				sqLiteDatabase.delete("password_group", "name = ?", new String[] { oldGroupName });
			} else {
				// 新的分组不存在， 更新旧的分组名称
				ContentValues contentValues = new ContentValues();
				contentValues.put("name", newGroupName);
				sqLiteDatabase.update("password_group", contentValues, "name = ?", new String[] { oldGroupName });
			}

			ContentValues contentValues = new ContentValues();
			contentValues.put("group_name", newGroupName);
			sqLiteDatabase.update("password", contentValues, "group_name = ?", new String[] { oldGroupName });
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (rawQuery != null)
				rawQuery.close();
		}
	}

	public int deletePasswordGroup(String passwordGroupName) {
		SQLiteDatabase sqLiteDatabase = getWritableDatabase();
		int count;
		count = sqLiteDatabase.delete("password_group", "name = ?", new String[] { passwordGroupName });
		if (count > 0) {
			sqLiteDatabase.delete("password", "group_name = ?", new String[] { passwordGroupName });
		}
		return count;
	}

	public String getCurrentPasswd() {
		return currentPasswd;
	}

	public void setCurrentPasswd(String currentPasswd) {
		this.currentPasswd = currentPasswd;
	}

	/** 加密 */
	public String encrypt(String key) {
		String result = "";
		try {
			result = AES.encrypt(key, currentPasswd);
		} catch (Exception e) {
			e.printStackTrace();
			result = key;
		}
		return result;
	}

	/** 解密 */
	public String decrypt(String data) {
		return decrypt(data, currentPasswd);
	}

	public String decrypt(String data, String passwd) {
		String result = "";
		try {
			result = AES.decrypt(data, passwd);
		} catch (Exception e) {
			e.printStackTrace();
			result = data;
		}
		return result;
	}

	/** 用户手势变化后，重新加密数据库保存的密码 */
	public void encodePasswd(String newPasswd) {
		SQLiteDatabase sqLiteDatabase = getWritableDatabase();
		List<Password> passwords = new ArrayList<Password>();

		// 获取数据库所有的密码
		Cursor cursor = null;
		try {
			cursor = sqLiteDatabase.rawQuery("select id, password from password", null);
			while (cursor.moveToNext()) {
				Password password = new Password();
				password.setId(cursor.getInt(cursor.getColumnIndex("id")));
				password.setPassword(decrypt(cursor.getString(cursor.getColumnIndex("password"))));
				passwords.add(password);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cursor != null)
				cursor.close();
		}

		currentPasswd = newPasswd;
		for (Password password : passwords) {
			ContentValues contentValues = new ContentValues();
			contentValues.put("password", encrypt(password.getPassword()));
			sqLiteDatabase.update("password", contentValues, "id = ?", new String[] { password.getId() + "" });
		}
	}
}
