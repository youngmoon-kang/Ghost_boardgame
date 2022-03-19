import socket
from _thread import *
import json
from util import *

def threaded(client_socket, addr):
    print('Connected by : ', addr[0], ':', addr[1])

    try:
        recv_data = client_socket.recv(1024).decode('utf-8')
        recv_data_json = json.loads(recv_data) #예시: {func: func_name, data:{id: "A10V"}}

        if not recv_data:
            print("Disconnected by " + addr[0], ":", addr[1])
            return

        print("Received from " + addr[0], ':', addr[1])

        func_name = recv_data_json['func']
        data = recv_data_json['data']

        print("func_name: {0}  data: {1}".format(func_name, data))

        result = 'false'

        # ================= DB연결 =======================

        # 중복id 있는지 확인 - 중복 데이터 있으면 false
        if (func_name == 'select_user'):
            user_id = data['id']
            select_result = select_user(user_id)
            print(len(select_result))
            if (len(select_result) == 0):
                result = 'true'
            else:
                result = 'false'

        # id DB에 삽입
        elif(func_name == 'insert_user'):
            user_id = data['id']
            ip = str(addr[0])
            if(insert_user(user_id, ip)):
                result = 'true'
            else:
                result = 'false'

        elif (func_name == 'delete_user'):
            user_id = data['id']
            if (delete_user(user_id)):
                result = 'true'
            else:
                result = 'false'

        elif (func_name == 'make_room'):
            room_id = get_new_room_id()
            room_name = data['name']
            use_password = data['use_password']
            password = data['password']
            user_id = data['user']
            if (make_room(room_id, room_name, password, 1, "N", use_password) and enter_room(user_id, room_id)):
                result = 'true'
            else:
                result = 'false'

        elif func_name == "search_room":
            result = search_room()
            result = str(result) + '\n'
            client_socket.send(result.encode())
            print("result: " + result)
            client_socket.close()
            return

        elif func_name == "enter_room":
            room_id = data['room_id']
            user_id = data['user_id']
            if(enter_room(user_id, room_id)):
                num = count_room_player(room_id) #2명 있음 false
                if(num == False):
                    result = "false"
                else:
                    if update_room_player(room_id, num):
                        result = 'true'
                    else:
                        result = 'false'
            else:
                result = 'false'

        elif func_name == "password_ok":
            room_id = data['room_id']
            password = data['password']
            ret = password_ok(room_id, password)
            if ret:
                result = "true"
            else:
                result = 'false'

        elif (func_name == 'get_room_info'):
            room_id = data['id']
            try:
                result = str(get_room_info(room_id))
            except: #방 정보를 불러올 수 없을 때
                result = 'false'

        elif (func_name == 'exit_room'):
            try:
                result = str(room_exit(addr[0]))
            except: #방 정보를 불러올 수 없을 때
                result = 'false'

        else:
            result = 'func_name error!!'
        # ====================DB연결 종료===================

        client_socket.send(result.encode())

    except ConnectionResetError as e:
        print('Disconnected by ' + addr[0], ':', addr[1])

    client_socket.close()


HOST = ''
PORT = 9998

server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server_socket.bind((HOST, PORT))
server_socket.listen()

print('server start')

while True:
    print('wait')

    client_socket, addr = server_socket.accept()
    start_new_thread(threaded, (client_socket, addr))

server_socket.close()
