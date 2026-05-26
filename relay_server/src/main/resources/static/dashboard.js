
const host = window.location.host;
const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';

const socket = new WebSocket(`${protocol}//${host}/v1`);

socket.onmessage = (event) => {
    console.log('server event:', event.data);
};
