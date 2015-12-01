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
 * �������е���������
 *
 */
public class PasswordDatabase extends SQLiteOpenHelper {
	private static final int version = 4;
	private Context context;

	/** AES�����ܳ� */
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
	 * 1 ---> ��ʼ�汾������һ����password(id, create_date, title, user_name,
	 * password��note)
	 * <p>
	 * 2 ---> password�����is_top�ֶΣ�Ĭ��ֵΪ�㣬��ʾ���ö�
	 * <p>
	 * 3 --->password�����group_name�ֶΣ���ʾ���飬Ĭ��ֵ��Ĭ�ϣ�����group(name)�����ӷ��鹦��
	 * <p>
	 * 4 --->��������м���
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

	// ���Ĵ������� �������ݿ����������
	private void upgradeFor4(SQLiteDatabase db) {
		// ȡ�����ݿ��е���������
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
		// ��������ܺ󣬱�������
		for (Password password : passwords) {
			ContentValues contentValues = new ContentValues();
			contentValues.put("password", encrypt(password.getPassword()));
			db.update("password", contentValues, "id = ?", new String[] { password.getId() + "" });
		}
	}

	/**
	 * ����һ������
	 * 
	 * @param password
	 *            Ҫ�����ֵ
	 * @return �����������ݵ��������� �������ʧ�ܣ�����-1
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
	 * ��������
	 * 
	 * <pre>
	 * // ��������
	 * <code>
	 * Password password = new Password();
	 * password.setId(123);
	 * password.setPassword(&quot;�µ�����&quot;);
	 * passwordDatabase.updatePassword(password);
	 * </code>
	 * @param password
	 *            ���µ����ݣ�ֻ��Ҫ������Ӧ�ĸ�����,������id����
	 * @return Ӱ������� the number of rows affected
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
	 * ����id��ѯ���ݿ��е�������Ϣ
	 * 
	 * @param id
	 * @return ��ѯ��������Ϣ�����û�и����ݣ�����null
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
	 * ������ݿ��б��������������Ϣ
	 * 
	 * @return �������ݣ�Ϊһ������
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
	 * ɾ��һ������
	 * 
	 * @param id
	 *            ɾ����id
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
	 * ����groupName������ݿ��б���ĸ÷��������е�����
	 * 
	 * @param groupName
	 *            ������
	 * @return �������ݣ�Ϊһ������
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
	 * ���ݿ�������������
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
	 * ��ȡ���ݿ��е����з���
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
	 * ����������
	 * 
	 * @param oldGroupName
	 *            ������
	 * @param newGroupName
	 *            �µ�����
	 */
	public void updatePasswdGroupName(String oldGroupName, String newGroupName) {
		SQLiteDatabase sqLiteDatabase = getWritableDatabase();
		Cursor rawQuery = null;
		try {
			rawQuery = sqLiteDatabase.rawQuery("select count(name) from password_group where name = ?",
					new String[] { newGroupName });
			if (rawQuery != null && rawQuery.moveToNext() && rawQuery.getInt(0) == 1) {
				// �µķ����Ѿ����� ֱ��ɾ���ɵķ���
				sqLiteDatabase.delete("password_group", "name = ?", new String[] { oldGroupName });
			} else {
				// �µķ��鲻���ڣ� ���¾ɵķ�������
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

	/** ���� */
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

	/** ���� */
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

	/** �û����Ʊ仯�����¼������ݿⱣ������� */
	public void encodePasswd(String newPasswd) {
		SQLiteDatabase sqLiteDatabase = getWritableDatabase();
		List<Password> passwords = new ArrayList<Password>();

		// ��ȡ���ݿ����е�����
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
