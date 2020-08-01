provider "mysql" {
  endpoint = "${data.terraform_remote_state.shared.outputs.db_host}:${data.terraform_remote_state.shared.outputs.db_port}"
  username = data.terraform_remote_state.shared.outputs.db_master_username
  password = data.terraform_remote_state.shared.outputs.db_master_password
}

resource "mysql_database" "db" {
  name = "${var.app_env_tag}-db"
}

resource "random_password" "password" {
  length = 16
}

resource "mysql_user" "user" {
  user = "${var.app_env_tag}-user"
  host = "%"
  plaintext_password = random_password.password.result
}

resource "mysql_grant" "grant" {
  user = mysql_user.user.user
  host = mysql_user.user.host
  database = mysql_database.db.name
  privileges = ["all"]
}
