using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

#pragma warning disable CA1814 // Prefer jagged arrays over multidimensional

namespace ClimbBetter.Infrastructure.Migrations
{
    /// <inheritdoc />
    public partial class SeedLocationsAndClimbs : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.InsertData(
                schema: "training",
                table: "Climbs",
                columns: new[] { "Id", "DifficultyId", "IsPublic", "LengthMeters", "LocationId", "Name", "OwnerId", "SuggestedMoveCount", "Type" },
                values: new object[,]
                {
                    { new Guid("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1"), 2, true, null, new Guid("11111111-1111-1111-1111-111111111111"), "Żółta 1", null, 7, 1 },
                    { new Guid("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2"), 4, true, null, new Guid("11111111-1111-1111-1111-111111111111"), "Żółta 2", null, 7, 1 },
                    { new Guid("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3"), 8, true, null, new Guid("11111111-1111-1111-1111-111111111111"), "Żółta 3", null, 7, 1 },
                    { new Guid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1"), 2, true, 22, new Guid("22222222-2222-2222-2222-222222222222"), "Ryski nad tablicą", null, 44, 0 },
                    { new Guid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2"), 4, true, 20, new Guid("22222222-2222-2222-2222-222222222222"), "Filar Wallischa", null, 40, 0 },
                    { new Guid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3"), 7, true, 22, new Guid("22222222-2222-2222-2222-222222222222"), "Zacięcie w Abazym", null, 44, 0 },
                    { new Guid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb4"), 7, true, 22, new Guid("22222222-2222-2222-2222-222222222222"), "Ryski Myszkowskiego", null, 44, 0 },
                    { new Guid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb5"), 8, true, 22, new Guid("22222222-2222-2222-2222-222222222222"), "Abazy Pasza", null, 44, 0 },
                    { new Guid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb6"), 8, true, 22, new Guid("22222222-2222-2222-2222-222222222222"), "Zacięcie w Abazym wprost", null, 44, 0 }
                });

            migrationBuilder.InsertData(
                schema: "training",
                table: "Locations",
                columns: new[] { "Id", "Area", "Continent", "Country", "IsPublic", "Name", "OwnerId", "Sector", "SubArea", "Type" },
                values: new object[,]
                {
                    { new Guid("11111111-1111-1111-1111-111111111111"), "Kraków", "Europe", "PL", true, "Bronx", null, null, null, 2 },
                    { new Guid("22222222-2222-2222-2222-222222222222"), "Jura Krakowsko-Częstochowska", "Europe", "PL", true, "Filar Abazego", null, "Dolina Bolechowicka", "Filar Abazego", 3 }
                });
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DeleteData(
                schema: "training",
                table: "Climbs",
                keyColumn: "Id",
                keyValue: new Guid("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1"));

            migrationBuilder.DeleteData(
                schema: "training",
                table: "Climbs",
                keyColumn: "Id",
                keyValue: new Guid("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2"));

            migrationBuilder.DeleteData(
                schema: "training",
                table: "Climbs",
                keyColumn: "Id",
                keyValue: new Guid("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3"));

            migrationBuilder.DeleteData(
                schema: "training",
                table: "Climbs",
                keyColumn: "Id",
                keyValue: new Guid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1"));

            migrationBuilder.DeleteData(
                schema: "training",
                table: "Climbs",
                keyColumn: "Id",
                keyValue: new Guid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2"));

            migrationBuilder.DeleteData(
                schema: "training",
                table: "Climbs",
                keyColumn: "Id",
                keyValue: new Guid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3"));

            migrationBuilder.DeleteData(
                schema: "training",
                table: "Climbs",
                keyColumn: "Id",
                keyValue: new Guid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb4"));

            migrationBuilder.DeleteData(
                schema: "training",
                table: "Climbs",
                keyColumn: "Id",
                keyValue: new Guid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb5"));

            migrationBuilder.DeleteData(
                schema: "training",
                table: "Climbs",
                keyColumn: "Id",
                keyValue: new Guid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb6"));

            migrationBuilder.DeleteData(
                schema: "training",
                table: "Locations",
                keyColumn: "Id",
                keyValue: new Guid("11111111-1111-1111-1111-111111111111"));

            migrationBuilder.DeleteData(
                schema: "training",
                table: "Locations",
                keyColumn: "Id",
                keyValue: new Guid("22222222-2222-2222-2222-222222222222"));
        }
    }
}
