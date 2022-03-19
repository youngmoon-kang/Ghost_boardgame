import pymysql
import random

db_connection = pymysql.connect(user='root',
                                passwd='password',
                                host='127.0.0.1',
                                port=3307,
                                db='boardgame_ghost',
                                charset='utf8'
                                )

cursor = db_connection.cursor(pymysql.cursors.DictCursor)

def insert_user(user_id, ip):
    sql = '''INSERT INTO user(id, ip) VALUES('{0}', '{1}') ON DUPLICATE KEY UPDATE id = '{0}';'''.format(user_id, ip)
    print(sql)

    try:
        cursor.execute(sql)
        db_connection.commit()

    except:
        print("error")
        return False

    return True

def delete_user(user_id):
    sql = '''DELETE FROM user WHERE id = '{0}';'''.format(user_id)
    print(sql)

    try:
        cursor.execute(sql)
        db_connection.commit()

    except:
        print("error")
        return False

    return True

def select_user(user_id):
    sql = '''SELECT * FROM user WHERE id LIKE '{0}';'''.format(user_id)
    cursor.execute(sql)
    result = cursor.fetchall()

    return result

def get_new_room_id():
    id = ''
    alpha = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
    result = (' ')

    while len(result) != 0:
        print(len(result))
        id = ''

        for i in range(3):
            tmp = random.randint(0, len(alpha) - 1)
            id += alpha[tmp]

        sql = '''SELECT roomid FROM room WHERE roomid = '{0}';'''.format(id)
        cursor.execute(sql)
        result = cursor.fetchall()
        print(id)

    return id

def make_room(room_id, room_name, password, currentcnt, playing, use_password):
    sql = '''INSERT INTO room(roomid, roomname, password, currentcnt, playing, use_password) 
    VALUES('{0}', '{1}', '{2}', {3}, '{4}', '{5}');'''.format(room_id, room_name, password, currentcnt, playing, use_password)
    print(sql)

    try:
        cursor.execute(sql)
        db_connection.commit()
    except pymysql.err.InternalError as e:
        print(e.args)
        return False

    return True

def search_room():
    sql = '''SELECT roomid as id, roomname as name, playing, currentcnt as playerCnt, use_password FROM room'''

    cursor.execute(sql)
    db_connection.commit()
    result = cursor.fetchall()
    result_dict = dict()
    result_dict["room_list"] = result

    return result_dict

def enter_room(user_id, room_id):
    sql = '''UPDATE user SET roomid = '{0}' WHERE id = '{1}';'''.format(room_id, user_id)
    try:
        cursor.execute(sql)
        db_connection.commit()
    except pymysql.err.InternalError as e:
        print(e.args)
        return False

    return True

def count_room_player(room_id):
    sql = '''SELECT COUNT(*) as cnt FROM user WHERE roomid = '{0}';'''.format(room_id)
    try:
        cursor.execute(sql)
        db_connection.commit()
        result = cursor.fetchall()
        result = result[0]['cnt']
    except:
        print("error")
        return False

    return result

def update_room_player(room_id, num):
    sql = '''UPDATE room SET currentcnt = {0} WHERE roomid = '{1}';'''.format(num, room_id)
    try:
        cursor.execute(sql)
        db_connection.commit()
    except:
        print("error")
        return False

    return True

def password_ok(room_id, password):
    sql = '''SELECT * FROM room WHERE roomid = '{0}' and password = '{1}';'''.format(room_id, password)

    cursor.execute(sql)
    db_connection.commit()
    result = cursor.fetchall()
    if len(result) == 0:
        return False
    else:
        return True

def get_room_info(room_id):
    sql = '''SELECT roomid as id, roomname as name, playing, currentcnt as playerCnt FROM room WHERE roomid = '{0}';'''.format(room_id)

    cursor.execute(sql)
    db_connection.commit()
    result = cursor.fetchall()
    result = result[0]

    return result

def get_room_id(ip):
    sql = '''SELECT roomid as room_id FROM user WHERE ip = '{0}';'''.format(ip)

    cursor.execute(sql)
    db_connection.commit()
    result = cursor.fetchall()
    result = result[0]['room_id']

    return result

def update_room_playing(room_id, playing):
    sql = '''UPDATE room SET playing = '{0}' WHERE roomid = '{1}';'''.format(playing, room_id)

    try:
        cursor.execute(sql)
        db_connection.commit()
    except:
        print("error")
        return False
    return True

def room_exit(user_ip):
    sql = '''call P_ROOM_EXIT('{0}');'''.format(user_ip)

    try:
        cursor.execute(sql)
        db_connection.commit()
    except:
        print("error")
        return False
    return True

if __name__ == "__main__":
    print(get_new_room_id())
    #make_room("1", "test", "", 1, "N", "AAA", "N")