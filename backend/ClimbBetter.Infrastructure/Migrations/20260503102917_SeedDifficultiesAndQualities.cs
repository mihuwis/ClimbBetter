using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

#pragma warning disable CA1814 // Prefer jagged arrays over multidimensional

namespace ClimbBetter.Infrastructure.Migrations
{
    /// <inheritdoc />
    public partial class SeedDifficultiesAndQualities : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.InsertData(
                schema: "training",
                table: "Difficulties",
                columns: new[] { "Id", "Name", "Points" },
                values: new object[,]
                {
                    { 1, "5c", 55 },
                    { 2, "6a", 60 },
                    { 3, "6a+", 65 },
                    { 4, "6b", 70 },
                    { 5, "6b+", 75 },
                    { 6, "6c", 80 },
                    { 7, "6c+", 85 },
                    { 8, "7a", 90 }
                });

            migrationBuilder.InsertData(
                schema: "training",
                table: "Qualities",
                columns: new[] { "Id", "Code", "Multiplier", "Name" },
                values: new object[,]
                {
                    { 1, "OS", 1.20m, "Onsight" },
                    { 2, "FL", 1.10m, "Flash" },
                    { 3, "RP", 1.00m, "Redpoint" },
                    { 4, "RRP", 0.80m, "Repeat RP" },
                    { 5, "ATT", 1.00m, "Attempt" },
                    { 6, "RTR", 0.50m, "Repeat TR" }
                });
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DeleteData(
                schema: "training",
                table: "Difficulties",
                keyColumn: "Id",
                keyValue: 1);

            migrationBuilder.DeleteData(
                schema: "training",
                table: "Difficulties",
                keyColumn: "Id",
                keyValue: 2);

            migrationBuilder.DeleteData(
                schema: "training",
                table: "Difficulties",
                keyColumn: "Id",
                keyValue: 3);

            migrationBuilder.DeleteData(
                schema: "training",
                table: "Difficulties",
                keyColumn: "Id",
                keyValue: 4);

            migrationBuilder.DeleteData(
                schema: "training",
                table: "Difficulties",
                keyColumn: "Id",
                keyValue: 5);

            migrationBuilder.DeleteData(
                schema: "training",
                table: "Difficulties",
                keyColumn: "Id",
                keyValue: 6);

            migrationBuilder.DeleteData(
                schema: "training",
                table: "Difficulties",
                keyColumn: "Id",
                keyValue: 7);

            migrationBuilder.DeleteData(
                schema: "training",
                table: "Difficulties",
                keyColumn: "Id",
                keyValue: 8);

            migrationBuilder.DeleteData(
                schema: "training",
                table: "Qualities",
                keyColumn: "Id",
                keyValue: 1);

            migrationBuilder.DeleteData(
                schema: "training",
                table: "Qualities",
                keyColumn: "Id",
                keyValue: 2);

            migrationBuilder.DeleteData(
                schema: "training",
                table: "Qualities",
                keyColumn: "Id",
                keyValue: 3);

            migrationBuilder.DeleteData(
                schema: "training",
                table: "Qualities",
                keyColumn: "Id",
                keyValue: 4);

            migrationBuilder.DeleteData(
                schema: "training",
                table: "Qualities",
                keyColumn: "Id",
                keyValue: 5);

            migrationBuilder.DeleteData(
                schema: "training",
                table: "Qualities",
                keyColumn: "Id",
                keyValue: 6);
        }
    }
}
