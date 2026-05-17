using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ClimbBetter.Infrastructure.Migrations
{
    /// <inheritdoc />
    public partial class AddQualityCode : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "Code",
                schema: "training",
                table: "Qualities",
                type: "text",
                nullable: false,
                defaultValue: "");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "Code",
                schema: "training",
                table: "Qualities");
        }
    }
}
