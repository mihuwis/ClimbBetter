using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ClimbBetter.Infrastructure.Migrations
{
    /// <inheritdoc />
    public partial class AddDurationToTrainingSession : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "OwnerId",
                schema: "training",
                table: "TrainingSessions");

            migrationBuilder.DropColumn(
                name: "Type",
                schema: "training",
                table: "TrainingSessions");

            migrationBuilder.RenameColumn(
                name: "SessionId",
                schema: "training",
                table: "TrainingEntries",
                newName: "TrainingSessionId");

            migrationBuilder.RenameColumn(
                name: "CompletedMoveCount",
                schema: "training",
                table: "TrainingEntries",
                newName: "ExecutedMoves");

            migrationBuilder.RenameColumn(
                name: "CalculatedPoints",
                schema: "training",
                table: "TrainingEntries",
                newName: "DifficultyPointsSnapshot");

            migrationBuilder.AlterColumn<TimeSpan>(
                name: "Duration",
                schema: "training",
                table: "TrainingSessions",
                type: "interval",
                nullable: true,
                oldClrType: typeof(TimeSpan),
                oldType: "interval");

            migrationBuilder.AddColumn<string>(
                name: "Goal",
                schema: "training",
                table: "TrainingSessions",
                type: "text",
                nullable: true);

            migrationBuilder.AddColumn<Guid>(
                name: "LocationId",
                schema: "training",
                table: "TrainingSessions",
                type: "uuid",
                nullable: true);

            migrationBuilder.AddColumn<Guid>(
                name: "MesocycleId",
                schema: "training",
                table: "TrainingSessions",
                type: "uuid",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "Method",
                schema: "training",
                table: "TrainingSessions",
                type: "text",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "AscentCategory",
                schema: "training",
                table: "TrainingEntries",
                type: "character varying(50)",
                maxLength: 50,
                nullable: false,
                defaultValue: "");

            migrationBuilder.AddColumn<int>(
                name: "AttemptNumberOnClimb",
                schema: "training",
                table: "TrainingEntries",
                type: "integer",
                nullable: true);

            migrationBuilder.AddColumn<Guid>(
                name: "ClientEntryId",
                schema: "training",
                table: "TrainingEntries",
                type: "uuid",
                nullable: true);

            migrationBuilder.AddColumn<DateTime>(
                name: "CreatedAt",
                schema: "training",
                table: "TrainingEntries",
                type: "timestamp with time zone",
                nullable: false,
                defaultValue: new DateTime(1, 1, 1, 0, 0, 0, 0, DateTimeKind.Unspecified));

            migrationBuilder.AddColumn<int>(
                name: "DifficultyId",
                schema: "training",
                table: "TrainingEntries",
                type: "integer",
                nullable: false,
                defaultValue: 0);

            migrationBuilder.AddColumn<bool>(
                name: "IsSuccessful",
                schema: "training",
                table: "TrainingEntries",
                type: "boolean",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<decimal>(
                name: "LengthMultiplierSnapshot",
                schema: "training",
                table: "TrainingEntries",
                type: "numeric(10,2)",
                precision: 10,
                scale: 2,
                nullable: false,
                defaultValue: 0m);

            migrationBuilder.AddColumn<string>(
                name: "Notes",
                schema: "training",
                table: "TrainingEntries",
                type: "character varying(1000)",
                maxLength: 1000,
                nullable: true);

            migrationBuilder.AddColumn<decimal>(
                name: "PointsSnapshot",
                schema: "training",
                table: "TrainingEntries",
                type: "numeric(12,2)",
                precision: 12,
                scale: 2,
                nullable: false,
                defaultValue: 0m);

            migrationBuilder.AddColumn<decimal>(
                name: "QualityMultiplierSnapshot",
                schema: "training",
                table: "TrainingEntries",
                type: "numeric(10,2)",
                precision: 10,
                scale: 2,
                nullable: false,
                defaultValue: 0m);

            migrationBuilder.AddColumn<int>(
                name: "TotalMoves",
                schema: "training",
                table: "TrainingEntries",
                type: "integer",
                nullable: true);

            migrationBuilder.CreateIndex(
                name: "IX_TrainingSessions_LocationId",
                schema: "training",
                table: "TrainingSessions",
                column: "LocationId");

            migrationBuilder.CreateIndex(
                name: "IX_TrainingEntries_ClimbId",
                schema: "training",
                table: "TrainingEntries",
                column: "ClimbId");

            migrationBuilder.CreateIndex(
                name: "IX_TrainingEntries_DifficultyId",
                schema: "training",
                table: "TrainingEntries",
                column: "DifficultyId");

            migrationBuilder.CreateIndex(
                name: "IX_TrainingEntries_QualityId",
                schema: "training",
                table: "TrainingEntries",
                column: "QualityId");

            migrationBuilder.CreateIndex(
                name: "IX_TrainingEntries_TrainingSessionId",
                schema: "training",
                table: "TrainingEntries",
                column: "TrainingSessionId");

            migrationBuilder.AddForeignKey(
                name: "FK_TrainingEntries_Climbs_ClimbId",
                schema: "training",
                table: "TrainingEntries",
                column: "ClimbId",
                principalSchema: "training",
                principalTable: "Climbs",
                principalColumn: "Id",
                onDelete: ReferentialAction.Restrict);

            migrationBuilder.AddForeignKey(
                name: "FK_TrainingEntries_Difficulties_DifficultyId",
                schema: "training",
                table: "TrainingEntries",
                column: "DifficultyId",
                principalSchema: "training",
                principalTable: "Difficulties",
                principalColumn: "Id",
                onDelete: ReferentialAction.Restrict);

            migrationBuilder.AddForeignKey(
                name: "FK_TrainingEntries_Qualities_QualityId",
                schema: "training",
                table: "TrainingEntries",
                column: "QualityId",
                principalSchema: "training",
                principalTable: "Qualities",
                principalColumn: "Id",
                onDelete: ReferentialAction.Restrict);

            migrationBuilder.AddForeignKey(
                name: "FK_TrainingEntries_TrainingSessions_TrainingSessionId",
                schema: "training",
                table: "TrainingEntries",
                column: "TrainingSessionId",
                principalSchema: "training",
                principalTable: "TrainingSessions",
                principalColumn: "Id",
                onDelete: ReferentialAction.Cascade);

            migrationBuilder.AddForeignKey(
                name: "FK_TrainingSessions_Locations_LocationId",
                schema: "training",
                table: "TrainingSessions",
                column: "LocationId",
                principalSchema: "training",
                principalTable: "Locations",
                principalColumn: "Id");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropForeignKey(
                name: "FK_TrainingEntries_Climbs_ClimbId",
                schema: "training",
                table: "TrainingEntries");

            migrationBuilder.DropForeignKey(
                name: "FK_TrainingEntries_Difficulties_DifficultyId",
                schema: "training",
                table: "TrainingEntries");

            migrationBuilder.DropForeignKey(
                name: "FK_TrainingEntries_Qualities_QualityId",
                schema: "training",
                table: "TrainingEntries");

            migrationBuilder.DropForeignKey(
                name: "FK_TrainingEntries_TrainingSessions_TrainingSessionId",
                schema: "training",
                table: "TrainingEntries");

            migrationBuilder.DropForeignKey(
                name: "FK_TrainingSessions_Locations_LocationId",
                schema: "training",
                table: "TrainingSessions");

            migrationBuilder.DropIndex(
                name: "IX_TrainingSessions_LocationId",
                schema: "training",
                table: "TrainingSessions");

            migrationBuilder.DropIndex(
                name: "IX_TrainingEntries_ClimbId",
                schema: "training",
                table: "TrainingEntries");

            migrationBuilder.DropIndex(
                name: "IX_TrainingEntries_DifficultyId",
                schema: "training",
                table: "TrainingEntries");

            migrationBuilder.DropIndex(
                name: "IX_TrainingEntries_QualityId",
                schema: "training",
                table: "TrainingEntries");

            migrationBuilder.DropIndex(
                name: "IX_TrainingEntries_TrainingSessionId",
                schema: "training",
                table: "TrainingEntries");

            migrationBuilder.DropColumn(
                name: "Goal",
                schema: "training",
                table: "TrainingSessions");

            migrationBuilder.DropColumn(
                name: "LocationId",
                schema: "training",
                table: "TrainingSessions");

            migrationBuilder.DropColumn(
                name: "MesocycleId",
                schema: "training",
                table: "TrainingSessions");

            migrationBuilder.DropColumn(
                name: "Method",
                schema: "training",
                table: "TrainingSessions");

            migrationBuilder.DropColumn(
                name: "AscentCategory",
                schema: "training",
                table: "TrainingEntries");

            migrationBuilder.DropColumn(
                name: "AttemptNumberOnClimb",
                schema: "training",
                table: "TrainingEntries");

            migrationBuilder.DropColumn(
                name: "ClientEntryId",
                schema: "training",
                table: "TrainingEntries");

            migrationBuilder.DropColumn(
                name: "CreatedAt",
                schema: "training",
                table: "TrainingEntries");

            migrationBuilder.DropColumn(
                name: "DifficultyId",
                schema: "training",
                table: "TrainingEntries");

            migrationBuilder.DropColumn(
                name: "IsSuccessful",
                schema: "training",
                table: "TrainingEntries");

            migrationBuilder.DropColumn(
                name: "LengthMultiplierSnapshot",
                schema: "training",
                table: "TrainingEntries");

            migrationBuilder.DropColumn(
                name: "Notes",
                schema: "training",
                table: "TrainingEntries");

            migrationBuilder.DropColumn(
                name: "PointsSnapshot",
                schema: "training",
                table: "TrainingEntries");

            migrationBuilder.DropColumn(
                name: "QualityMultiplierSnapshot",
                schema: "training",
                table: "TrainingEntries");

            migrationBuilder.DropColumn(
                name: "TotalMoves",
                schema: "training",
                table: "TrainingEntries");

            migrationBuilder.RenameColumn(
                name: "TrainingSessionId",
                schema: "training",
                table: "TrainingEntries",
                newName: "SessionId");

            migrationBuilder.RenameColumn(
                name: "ExecutedMoves",
                schema: "training",
                table: "TrainingEntries",
                newName: "CompletedMoveCount");

            migrationBuilder.RenameColumn(
                name: "DifficultyPointsSnapshot",
                schema: "training",
                table: "TrainingEntries",
                newName: "CalculatedPoints");

            migrationBuilder.AlterColumn<TimeSpan>(
                name: "Duration",
                schema: "training",
                table: "TrainingSessions",
                type: "interval",
                nullable: false,
                defaultValue: new TimeSpan(0, 0, 0, 0, 0),
                oldClrType: typeof(TimeSpan),
                oldType: "interval",
                oldNullable: true);

            migrationBuilder.AddColumn<Guid>(
                name: "OwnerId",
                schema: "training",
                table: "TrainingSessions",
                type: "uuid",
                nullable: false,
                defaultValue: new Guid("00000000-0000-0000-0000-000000000000"));

            migrationBuilder.AddColumn<string>(
                name: "Type",
                schema: "training",
                table: "TrainingSessions",
                type: "text",
                nullable: false,
                defaultValue: "");
        }
    }
}
