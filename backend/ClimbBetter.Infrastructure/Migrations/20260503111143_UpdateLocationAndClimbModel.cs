using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ClimbBetter.Infrastructure.Migrations
{
    /// <inheritdoc />
    public partial class UpdateLocationAndClimbModel : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.RenameColumn(
                name: "DefaultMoveCount",
                schema: "training",
                table: "Climbs",
                newName: "SuggestedMoveCount");

            migrationBuilder.AlterColumn<Guid>(
                name: "OwnerId",
                schema: "training",
                table: "Locations",
                type: "uuid",
                nullable: true,
                oldClrType: typeof(Guid),
                oldType: "uuid");

            migrationBuilder.AddColumn<string>(
                name: "Area",
                schema: "training",
                table: "Locations",
                type: "text",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "Continent",
                schema: "training",
                table: "Locations",
                type: "text",
                nullable: false,
                defaultValue: "");

            migrationBuilder.AddColumn<string>(
                name: "Country",
                schema: "training",
                table: "Locations",
                type: "text",
                nullable: false,
                defaultValue: "");

            migrationBuilder.AddColumn<bool>(
                name: "IsPublic",
                schema: "training",
                table: "Locations",
                type: "boolean",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<string>(
                name: "Sector",
                schema: "training",
                table: "Locations",
                type: "text",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "SubArea",
                schema: "training",
                table: "Locations",
                type: "text",
                nullable: true);

            migrationBuilder.AddColumn<int>(
                name: "Type",
                schema: "training",
                table: "Locations",
                type: "integer",
                nullable: false,
                defaultValue: 0);

            migrationBuilder.AlterColumn<Guid>(
                name: "OwnerId",
                schema: "training",
                table: "Climbs",
                type: "uuid",
                nullable: true,
                oldClrType: typeof(Guid),
                oldType: "uuid");

            migrationBuilder.AddColumn<bool>(
                name: "IsPublic",
                schema: "training",
                table: "Climbs",
                type: "boolean",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<int>(
                name: "LengthMeters",
                schema: "training",
                table: "Climbs",
                type: "integer",
                nullable: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "Area",
                schema: "training",
                table: "Locations");

            migrationBuilder.DropColumn(
                name: "Continent",
                schema: "training",
                table: "Locations");

            migrationBuilder.DropColumn(
                name: "Country",
                schema: "training",
                table: "Locations");

            migrationBuilder.DropColumn(
                name: "IsPublic",
                schema: "training",
                table: "Locations");

            migrationBuilder.DropColumn(
                name: "Sector",
                schema: "training",
                table: "Locations");

            migrationBuilder.DropColumn(
                name: "SubArea",
                schema: "training",
                table: "Locations");

            migrationBuilder.DropColumn(
                name: "Type",
                schema: "training",
                table: "Locations");

            migrationBuilder.DropColumn(
                name: "IsPublic",
                schema: "training",
                table: "Climbs");

            migrationBuilder.DropColumn(
                name: "LengthMeters",
                schema: "training",
                table: "Climbs");

            migrationBuilder.RenameColumn(
                name: "SuggestedMoveCount",
                schema: "training",
                table: "Climbs",
                newName: "DefaultMoveCount");

            migrationBuilder.AlterColumn<Guid>(
                name: "OwnerId",
                schema: "training",
                table: "Locations",
                type: "uuid",
                nullable: false,
                defaultValue: new Guid("00000000-0000-0000-0000-000000000000"),
                oldClrType: typeof(Guid),
                oldType: "uuid",
                oldNullable: true);

            migrationBuilder.AlterColumn<Guid>(
                name: "OwnerId",
                schema: "training",
                table: "Climbs",
                type: "uuid",
                nullable: false,
                defaultValue: new Guid("00000000-0000-0000-0000-000000000000"),
                oldClrType: typeof(Guid),
                oldType: "uuid",
                oldNullable: true);
        }
    }
}
