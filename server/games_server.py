import socket
from _thread import *
import json
import threading
from util import *

def receive_ready_thread(player, opp_player): #준비완료 상대방에게 보냄
    try:
        recv_data = player.socket.recv(1024).decode('utf-8')
        recv_data_json = json.loads(recv_data)  # 예시: {state: "ready", red_point: 1,3/0,4/...}
    except:
        player.exit()
        return
    if recv_data_json['state'] == 'ready':
        print(recv_data_json)
        data = 'ready' + ' ' + recv_data_json['red_point'] + '\n'
        opp_player.socket.send(data.encode())
        return True

    elif recv_data_json['state'] == 'exit':
        player.exit()
        return

def move_thread(player, opp_player):
    while True:
        try:
            recv_data = player.socket.recv(1024).decode('utf-8')
            recv_data_json = json.loads(recv_data)  # 예시: {state: "move" from: '2,3' to: '2,4'}
        except:
            player.exit()
            return
        if recv_data_json['state'] == 'move':
            ret = recv_data_json['from'] + '/' + recv_data_json['to'] #'2,3/2,4'
            print(recv_data_json)

            data = 'move' + ' ' + ret + '\n' #'move 2,3/2,4\n'
            print(data)
            opp_player.socket.send(data.encode())

        elif recv_data_json['state'] == 'exit':
            print('exit')
            player.exit()
            return
        
        elif recv_data_json['state'] == 'end':
            update_room_playing(player.room, "N")
            return

#게임 다 참여하면
def gameRoom(player1, player2):
    room_id = player1.room

    print('host: Connected by : ', player1.ip)
    print('guest: Connected by : ', player1.ip)

    while player1.join and player2.join:
        if player1.join and player2.join:
            #서로에게 참석했다고 알려줌
            host_socket = player1.socket
            guest_socket = player2.socket
            data = 'come\n'
            host_socket.send(data.encode())
            guest_socket.send(data.encode())
            print("ready")

        if player1.join and player2.join:
            #준비되면 상대방에게 ready문구를 보냄 + 자신의 말 정보를 보냄
            t1 = threading.Thread(target=receive_ready_thread, args=(player1, player2))
            t2 = threading.Thread(target=receive_ready_thread, args=(player2, player1))
            t1.start()
            t2.start()

            t1.join()
            t2.join()

        print("start")
        update_room_playing(room_id, "Y")

        if player1.join and player2.join:
            #서로에게 움직임 전송
            t1 = threading.Thread(target=move_thread, args=(player1, player2))
            t2 = threading.Thread(target=move_thread, args=(player2, player1))
            t1.start()
            t2.start()

            t1.join()
            t2.join()

        wt1 = threading.Thread(target=waiting_thread, args=(player1,))
        wt2 = threading.Thread(target=waiting_thread, args=(player2,))

        wt1.start()
        wt2.start()

def waiting_thread(player):
    try:
        recv_data = player.socket.recv(1024).decode('utf-8')
        recv_data_json = json.loads(recv_data)  # 예시: {state: "move" from: '2,3' to: '2,4'}
    except:
        player.exit()
        return

    if recv_data_json['state'] == 'exit':
        print('exit')
        player.exit()
        return

    else:
        return


# ================================================================================================
HOST = ''
PORT = 9999

server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server_socket.bind((HOST, PORT))
server_socket.listen()

clients = dict()


class Player:
    def __init__(self, socket, ip, room_id):
        self.socket = socket
        self.ip = ip
        self.room = room_id
        self.join = True#참여 여부

    def exit(self):
        global clients
        self.join = False #참여상태 X
        try:
            current_room = clients[self.room]
        except:
            return

        if len(current_room) == 2: #상대방이 있는경우
            if current_room[0].ip == self.ip:
                opp_player = current_room[1]
                del current_room[1]
            else:
                opp_player = current_room[0]
                del current_room[0]
            data = "exit\n"
            opp_player.socket.send(data.encode())

            room_exit(self.ip)

            clients[self.room] = current_room

        else:#자신밖에 안남아있는 경우
            del clients[self.room]
            room_exit(self.ip)

if __name__ == "__main__":
    print('server start')

    while True:
        print('wait')

        client_socket, addr = server_socket.accept()

        client_temp = dict()
        client_temp['socket'] = client_socket
        client_temp['addr'] = addr

        room_id = get_room_id(addr[0])

        temp_player = Player(client_socket, addr[0], room_id)

        wt = threading.Thread(target=waiting_thread, args=(temp_player,))
        wt.start()

        if room_id in clients:
            clients[room_id].append(temp_player)
        else:
            clients[room_id] = []
            clients[room_id].append(temp_player)

        if(len(clients[room_id]) == 2):
            print(clients[room_id])
            game_thread = threading.Thread(target=gameRoom, args=tuple(clients[room_id]))
            game_thread.start()

    server_socket.close()


