# vpn-experiment

Shows how to connect to VPC using VPN. 

* Instance OpenVPN client: `sudo apt install -y openvpn`
* `./tool.sh make-certificates` gitclones the easyrsa, creates server and client certificates, and imports them to ACM.
* `./tool.sh deploy` deploys the stack and prints the dummy instance IP address.
* `./tool.sh export` downloads the OpenVPN configuration (`config.ovpn`).
* `./tool.sh connect` connects to VPN (Ctrl+C to exit)
* ping the dummy instance
* `./tool.sh undeploy` undeploys the stack.
* `./tool.sh unmake-certificates` deletes the server and client certificates from ACM and locally.
